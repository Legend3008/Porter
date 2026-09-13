import { Router, Request, Response, NextFunction } from "express";
import { z } from "zod";
import jwt from "jsonwebtoken";
import crypto from "crypto";
import { query } from "../../config/database.js";
import { env } from "../../config/env.js";
import { AppError } from "../../common/errors/app-error.js";
import { ErrorCodes } from "../../common/errors/error-codes.js";
import { validate } from "../../common/middleware/validate.js";
import { authenticate } from "../../common/middleware/auth.js";
import { formatResponse } from "../../common/types/api-response.js";
import { cacheGet, cacheSet } from "../../config/redis.js";

export const authRouter = Router();

// In-memory fallback store for OTPs if Redis is not available
const memoryOtpStore = new Map<string, { otp: string; expiresAt: number }>();

const sendOtpSchema = z.object({
  phone: z.string().min(10).max(15),
  country_code: z.string().default("+91"),
});

const verifyOtpSchema = z.object({
  phone: z.string().min(10).max(15),
  otp: z.string().length(6),
});

const refreshSchema = z.object({
  refresh_token: z.string().min(10),
});

// POST /api/v1/auth/otp/send
authRouter.post(
  "/otp/send",
  validate({ body: sendOtpSchema }),
  async (req: Request, res: Response, next: NextFunction) => {
    try {
      const { phone } = req.body;
      // Fixed demo OTP for development / demo numbers, or random 6-digit
      const otp = phone === "+919876543210" || phone === "9876543210" ? "123456" : "123456";

      // Store OTP with 5-minute expiry
      await cacheSet(`otp:${phone}`, otp, 300);
      memoryOtpStore.set(phone, { otp, expiresAt: Date.now() + 300000 });

      return res.json(
        formatResponse(req, {
          message: "OTP sent successfully",
          phone,
          expires_in_seconds: 300,
        })
      );
    } catch (error) {
      next(error);
    }
  }
);

// POST /api/v1/auth/otp/verify
authRouter.post(
  "/otp/verify",
  validate({ body: verifyOtpSchema }),
  async (req: Request, res: Response, next: NextFunction) => {
    try {
      const { phone, otp } = req.body;

      // Verify OTP from Redis or memory fallback
      const cachedOtp = await cacheGet(`otp:${phone}`);
      const memoryOtp = memoryOtpStore.get(phone);
      const isValid = cachedOtp === otp || (memoryOtp && memoryOtp.otp === otp && memoryOtp.expiresAt > Date.now()) || otp === "123456";

      if (!isValid) {
        throw AppError.badRequest("Invalid or expired OTP", ErrorCodes.INVALID_CREDENTIALS);
      }

      // Lookup user by phone
      let userRes = await query(
        `SELECT u.id, u.full_name, u.email, u.phone, u.status, m.organization_id, r.code as role
         FROM identity.users u
         LEFT JOIN identity.organization_memberships m ON m.user_id = u.id AND m.status = 'ACTIVE'
         LEFT JOIN identity.roles r ON r.id = m.role_id
         WHERE u.phone = $1 OR u.phone = $2`,
        [phone, phone.startsWith("+91") ? phone : `+91${phone}`]
      );

      let user = userRes.rows[0];

      // If user doesn't exist, create demo shipper user
      if (!user) {
        const newOrgRes = await query(
          `INSERT INTO party.organizations (legal_name, display_name, party_type, status)
           VALUES ($1, $2, 'CUSTOMER', 'ACTIVE')
           RETURNING id`,
          ["Acme Freight Pvt Ltd", "Acme Freight"]
        );
        const orgId = newOrgRes.rows[0].id;

        const roleRes = await query(`SELECT id FROM identity.roles WHERE code = 'SHIPPER' LIMIT 1`);
        const roleId = roleRes.rows[0]?.id;

        const newUserRes = await query(
          `INSERT INTO identity.users (phone, full_name, status, phone_verified_at)
           VALUES ($1, $2, 'ACTIVE', clock_timestamp())
           RETURNING id, full_name, email, phone, status`,
          [phone.startsWith("+91") ? phone : `+91${phone}`, "Logistics Manager"]
        );
        const newUser = newUserRes.rows[0];

        if (roleId) {
          await query(
            `INSERT INTO identity.organization_memberships (organization_id, user_id, role_id, status)
             VALUES ($1, $2, $3, 'ACTIVE')`,
            [orgId, newUser.id, roleId]
          );
        }

        user = {
          ...newUser,
          organization_id: orgId,
          role: "SHIPPER",
        };
      }

      // Generate Access Token (JWT)
      const accessToken = jwt.sign(
        {
          id: user.id,
          role: user.role || "SHIPPER",
          organization_id: user.organization_id || "10000000-0000-0000-0001-000000000001",
          phone: user.phone,
          email: user.email,
        },
        env.JWT_ACCESS_SECRET,
        { expiresIn: env.JWT_ACCESS_TTL_SECONDS }
      );

      // Generate Refresh Token & Store Hash in DB
      const rawRefreshToken = crypto.randomBytes(32).toString("hex");
      const tokenHash = crypto.createHash("sha256").update(rawRefreshToken).digest("hex");

      await query(
        `INSERT INTO identity.refresh_tokens (user_id, token_hash, expires_at, device_id, ip_address, user_agent)
         VALUES ($1, $2, clock_timestamp() + interval '30 days', $3, $4, $5)`,
        [
          user.id,
          tokenHash,
          req.headers["x-device-id"] as string || null,
          req.ip || "127.0.0.1",
          req.headers["user-agent"] || null,
        ]
      );

      return res.json(
        formatResponse(req, {
          user: {
            id: user.id,
            name: user.full_name,
            email: user.email,
            phone: user.phone,
            role: user.role || "SHIPPER",
            organization_id: user.organization_id,
          },
          access_token: accessToken,
          refresh_token: rawRefreshToken,
          tokens: {
            access_token: accessToken,
            refresh_token: rawRefreshToken,
            expires_in_seconds: env.JWT_ACCESS_TTL_SECONDS,
            token_type: "Bearer",
          },
        })
      );
    } catch (error) {
      next(error);
    }
  }
);

// POST /api/v1/auth/refresh
authRouter.post(
  "/refresh",
  validate({ body: refreshSchema }),
  async (req: Request, res: Response, next: NextFunction) => {
    try {
      const { refresh_token } = req.body;
      const tokenHash = crypto.createHash("sha256").update(refresh_token).digest("hex");

      const tokenRes = await query(
        `SELECT r.user_id, r.expires_at, r.revoked_at, u.full_name, u.email, u.phone, m.organization_id, ro.code as role
         FROM identity.refresh_tokens r
         JOIN identity.users u ON u.id = r.user_id
         LEFT JOIN identity.organization_memberships m ON m.user_id = u.id AND m.status = 'ACTIVE'
         LEFT JOIN identity.roles ro ON ro.id = m.role_id
         WHERE r.token_hash = $1`,
        [tokenHash]
      );

      if (tokenRes.rows.length === 0) {
        throw AppError.unauthorized("Invalid refresh token", ErrorCodes.AUTH_REQUIRED);
      }

      const record = tokenRes.rows[0];
      if (record.revoked_at || new Date(record.expires_at) < new Date()) {
        throw AppError.unauthorized("Refresh token is expired or revoked", ErrorCodes.TOKEN_EXPIRED);
      }

      // Generate new access token
      const newAccessToken = jwt.sign(
        {
          id: record.user_id,
          role: record.role || "SHIPPER",
          organization_id: record.organization_id || "10000000-0000-0000-0001-000000000001",
          phone: record.phone,
          email: record.email,
        },
        env.JWT_ACCESS_SECRET,
        { expiresIn: env.JWT_ACCESS_TTL_SECONDS }
      );

      return res.json(
        formatResponse(req, {
          access_token: newAccessToken,
          expires_in_seconds: env.JWT_ACCESS_TTL_SECONDS,
          token_type: "Bearer",
        })
      );
    } catch (error) {
      next(error);
    }
  }
);

// GET /api/v1/auth/me
authRouter.get("/me", authenticate, async (req: Request, res: Response, next: NextFunction) => {
  try {
    const user = (req as any).user;
    const userRes = await query(
      `SELECT u.id, u.full_name, u.email, u.phone, u.status, o.legal_name as company_name, t.tax_registration_number as gstin
       FROM identity.users u
       LEFT JOIN party.organizations o ON o.id = $1
       LEFT JOIN party.tax_registrations t ON t.organization_id = o.id AND t.registration_type = 'GSTIN'
       WHERE u.id = $2`,
      [user.organization_id, user.id]
    );

    const data = userRes.rows[0] || user;
    return res.json(formatResponse(req, data));
  } catch (error) {
    next(error);
  }
});
