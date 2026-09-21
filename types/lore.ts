import { IdName } from "./springTypes";

export interface LoreUser {
  id: number;
  title: string;
  description: string;
  date: string;
  content: string;
  isAuthor: boolean;
  author: string;
  nonPublic: boolean;
  byAdmin: boolean;
  tags: IdName[];
}

export interface LoreList {
  id: number;
  title: string;
  description: string;
  date: string;
  author: string;
  byAdmin: boolean;
  nonPublic: boolean;
  tags: IdName[];
}

export interface Excerpt {
  loreId: number,
  title: string,
  content: string,
  href: string,
}

