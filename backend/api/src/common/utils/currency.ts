/**
 * Monetary utilities: Strict integer minor units (Paise).
 * 1 Rupee = 100 Paise.
 * Floating point operations are strictly prohibited for monetary calculations.
 */

export function rupeesToPaise(rupees: number): bigint {
  return BigInt(Math.round(rupees * 100));
}

export function paiseToRupees(paise: bigint | number): number {
  return Number(paise) / 100;
}

export function formatPaiseToRupeeString(paise: bigint | number): string {
  const rupees = paiseToRupees(paise);
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: 2,
  }).format(rupees);
}
