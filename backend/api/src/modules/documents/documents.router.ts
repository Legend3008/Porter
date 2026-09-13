import { Router, Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { v4 as uuidv4 } from 'uuid';
import { authenticateJwt } from '../../common/middleware/auth';
import { query, withTransaction } from '../../config/database';
import { AppError } from '../../common/errors/app-error';
import { ErrorCode } from '../../common/errors/error-codes';
import { successResponse } from '../../common/types/api-response';
import { validateBody } from '../../common/middleware/validate';

export const documentsRouter = Router();
documentsRouter.use(authenticateJwt);

const initUploadSchema = z.object({
  booking_id: z.string().uuid(),
  document_type: z.enum([
    'BILL_OF_LADING', 'DELIVERY_ORDER', 'GATE_PASS', 'COMMERCIAL_INVOICE',
    'PACKING_LIST', 'CUSTOMS_DECLARATION', 'PROOF_OF_DELIVERY', 'OTHER'
  ]),
  file_name: z.string().min(1),
  mime_type: z.string().min(1),
  file_size_bytes: z.number().int().positive()
});

// POST /api/v1/documents/upload-url
// Returns pre-signed storage URL and tracking key for direct object storage uploads
documentsRouter.post('/upload-url', validateBody(initUploadSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { booking_id, document_type, file_name, mime_type } = req.body;
    const documentId = uuidv4();
    const sanitizedFileName = file_name.replace(/[^a-zA-Z0-9._-]/g, '_');
    const storageKey = `bookings/${booking_id}/${document_type.toLowerCase()}/${documentId}-${sanitizedFileName}`;

    // Generate secure upload target (presigned URL simulation for production S3/GCS)
    const uploadUrl = `https://storage.porter-logistics.in/v1/upload/${encodeURIComponent(storageKey)}?signature=sim_${Date.now()}`;

    return res.json(
      successResponse({
        document_id: documentId,
        storage_key: storageKey,
        upload_url: uploadUrl,
        expires_in_seconds: 900,
        headers: {
          'Content-Type': mime_type
        }
      })
    );
  } catch (err) {
    next(err);
  }
});

const completeUploadSchema = z.object({
  id: z.string().uuid().optional(),
  booking_id: z.string().uuid(),
  trip_id: z.string().uuid().optional(),
  document_type: z.enum([
    'BILL_OF_LADING', 'DELIVERY_ORDER', 'GATE_PASS', 'COMMERCIAL_INVOICE',
    'PACKING_LIST', 'CUSTOMS_DECLARATION', 'PROOF_OF_DELIVERY', 'OTHER'
  ]),
  storage_key: z.string().min(1),
  file_name: z.string().min(1),
  mime_type: z.string().min(1),
  file_size_bytes: z.number().int().positive(),
  checksum_sha256: z.string().min(32)
});

// POST /api/v1/documents/complete
// Registers uploaded document in database with SHA-256 integrity checksum
documentsRouter.post('/complete', validateBody(completeUploadSchema), async (req: Request, res: Response, next: NextFunction) => {
  try {
    const body = req.body;
    const userId = req.user!.userId;

    const doc = await withTransaction(async (client) => {
      const docRes = await client.query(
        `INSERT INTO documents.documents (
          id, booking_id, trip_id, document_type, status,
          storage_key, file_name, mime_type, file_size_bytes,
          checksum_sha256, uploaded_by, is_verified
        ) VALUES (
          COALESCE($1, gen_random_uuid()), $2, $3, $4, 'UPLOADED',
          $5, $6, $7, $8, $9, $10, false
        ) RETURNING *`,
        [
          body.id || null,
          body.booking_id,
          body.trip_id || null,
          body.document_type,
          body.storage_key,
          body.file_name,
          body.mime_type,
          body.file_size_bytes,
          body.checksum_sha256,
          userId
        ]
      );

      const created = docRes.rows[0];

      // Record version 1
      await client.query(
        `INSERT INTO documents.document_versions (
          document_id, version_number, storage_key, file_size_bytes, checksum_sha256, uploaded_by
        ) VALUES ($1, 1, $2, $3, $4, $5)`,
        [created.id, body.storage_key, body.file_size_bytes, body.checksum_sha256, userId]
      );

      return created;
    });

    return res.status(201).json(successResponse(doc));
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/documents/:id/download-url
documentsRouter.get('/:id/download-url', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const docId = req.params.id;

    const result = await query(
      `SELECT id, storage_key, file_name, mime_type, file_size_bytes, status 
       FROM documents.documents WHERE id = $1`,
      [docId]
    );

    if (result.rows.length === 0) {
      throw AppError.notFound('Document not found', ErrorCode.DOCUMENT_NOT_FOUND);
    }

    const doc = result.rows[0];
    const downloadUrl = `https://storage.porter-logistics.in/v1/download/${encodeURIComponent(doc.storage_key)}?signature=sim_download_${Date.now()}`;

    return res.json(
      successResponse({
        document_id: doc.id,
        file_name: doc.file_name,
        mime_type: doc.mime_type,
        file_size_bytes: doc.file_size_bytes,
        download_url: downloadUrl,
        expires_in_seconds: 1800
      })
    );
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/documents/booking/:bookingId
documentsRouter.get('/booking/:bookingId', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const bookingId = req.params.bookingId;

    const result = await query(
      `SELECT 
        d.id,
        d.document_type,
        d.status,
        d.file_name,
        d.mime_type,
        d.file_size_bytes,
        d.checksum_sha256,
        d.is_verified,
        d.created_at,
        u.full_name as uploaded_by_name
       FROM documents.documents d
       LEFT JOIN identity.users u ON u.id = d.uploaded_by
       WHERE d.booking_id = $1
       ORDER BY d.created_at DESC`,
      [bookingId]
    );

    return res.json(successResponse(result.rows));
  } catch (err) {
    next(err);
  }
});
