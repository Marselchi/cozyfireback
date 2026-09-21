import { InlineBlocksDiff } from "@/lib/stores/editor-state-store";
import { Excerpt } from "./lore";

// auth
export interface JwtAuthResponse {
  accessToken: string;
}

export type ViewStatus = "viewed" | "unviewed" | "updated";

export interface LoginRequest {
  username: string;
  password: string;
}

export interface SignupRequest {
  username: string;
  email: string;
  password: string;
  code: string;
}

// room
export interface RoomContextResponse {
  id: number;
  name: string;
  description: string;
  url: string;
  creator: CreatorInfo;
  isCurrentUserCreator: boolean;
  memberCount: number;
}

export interface CreatorInfo {
  id: number;
  username: string;
}

export interface RoomCreateRequest {
  name: string;
  description?: string;
  url?: string;
}

export interface RoomUpdateRequest {
  name?: string;
  description?: string;
}

export interface IdName {
  id: number;
  name: string;
}

export interface LoreResponse {
  id: number;
  title: string;
  description: string;
  date: string;
  content: string;
  accountName: string;
  createdByRoomCreator: boolean;
  roles: IdName[];
  tags: IdName[];
}

export interface LoreUserResponse {
  id: number;
  title: string;
  description: string;
  date: string;
  content: string;
  isAuthor: boolean;
  accountName: string;
  nonPublic: boolean;
  createdByRoomCreator: boolean;
  tags: IdName[];
  viewCount: number;
}

export interface LoreUserInlineResponse {
  id: number;
  title: string;
  description: string;
  date: string;
  content: string;
  isAuthor: boolean;
  accountName: string;
  nonPublic: boolean;
  createdByRoomCreator: boolean;
  tags: IdName[];
  excerpts: Excerpt[];
  viewCount: number;
  questionCount: number;
}
export interface LoreListResponse {
  id: number;
  title: string;
  description: string;
  date: string;
  accountName: string;
  createdByRoomCreator: boolean;
  nonPublic: boolean;
  viewed: boolean | null;
  tags: IdName[];
}

export interface LoreListFilter {
  createdByRoomCreator: boolean | null;
  tagIds: number[] | null;
  title: string | null;
}

export interface LoreListFilterParams {
  createdByRoomCreator?: boolean | null;
  tagNames?: string[] | null;
  title?: string | null;
  status?: ViewStatus | null;
}

export interface PageableParams {
  page?: number;
  size?: number;
  sort?: string[];
}

export interface Paginated<T> {
  content: T[];
  page: PageMeta;
}

export interface PageMeta {
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export interface LoreRequest {
  title: string;
  description?: string;
  date?: string;
  content: string;
  blockChanges?: InlineBlocksDiff;
  roleIds?: number[];
  tagIds?: number[];
}

// session types
export interface IdNameBool {
  id: number;
  name: string;
  accepted: boolean | null;
}

export interface SessionListResponse {
  id: number;
  creatorId: number;
  creatorName: string;
  time: string; // ISO 8601 Instant
  description: string;
}

export interface SessionResponse extends SessionListResponse {
  participants: IdNameBool[];
}

export interface SessionRequest {
  time: string; // ISO 8601 Instant
  description: string;
  accountIds: number[];
}
