export type BlockType = "normal" | "chance";

export interface ChanceConfig {
  skill: string;
  threshold: number;
}

export interface BlockSummary {
  id: string;
  loreId: string;
  title: string;
  type: BlockType;
  contentPreview: string;
  hasChance: boolean;
  roles: string[];
}

export interface BlockDetail {
  id: string;
  loreId: string;
  title: string;
  content: string;
  type: BlockType;
  roles: string[];
  chance?: ChanceConfig;
}

export type AccessStatus = "pending" | "fail" | "passed" | "granted" | "denied";

export interface AccessRecord {
  id: string;
  blockId: string;
  userId: string;
  userName: string;
  status: AccessStatus;
  rollValue?: number;
  passed: boolean;
}

export interface NormalAccessRecord {
  id: string;
  blockId: string;
  userId: string;
  userName: string;
  hasAccess: boolean;
}

export interface Skill {
  key: string;
  label: string;
}

export interface WrappedBlock {
  content: string;
}
