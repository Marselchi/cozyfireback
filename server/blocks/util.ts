import { AccessRecord, AccessStatus, NormalAccessRecord } from "@/types/blocks";

export type RawAccessRecord = {
  id: string | number;
  blockId: string | number;
  userId: string | number;
  userName: string;
  status: string;
  rollValue?: number | null;
  passed: boolean;
};

export type RawNormalAccessRecord = {
  id: string | number;
  blockId: string | number;
  userId: string | number;
  userName: string;
  hasAccess: boolean;
};

function toAccessStatus(value: string): AccessStatus {
  const normalized = value.toLowerCase();

  switch (normalized) {
    case "pending":
      return "pending";
    case "fail":
    case "failed":
      return "fail";
    case "passed":
      return "passed";
    case "granted":
      return "granted";
    case "denied":
      return "denied";
    default:
      return "pending";
  }
}

export function mapAccessRecord(raw: RawAccessRecord): AccessRecord {
  return {
    id: String(raw.id),
    blockId: String(raw.blockId),
    userId: String(raw.userId),
    userName: raw.userName ?? "",
    status: toAccessStatus(raw.status),
    rollValue: raw.rollValue ?? undefined,
    passed: Boolean(raw.passed),
  };
}

export function mapNormalAccessRecord(
  raw: RawNormalAccessRecord,
): NormalAccessRecord {
  return {
    id: String(raw.id),
    blockId: String(raw.blockId),
    userId: String(raw.userId),
    userName: raw.userName ?? "",
    hasAccess: Boolean(raw.hasAccess),
  };
}
