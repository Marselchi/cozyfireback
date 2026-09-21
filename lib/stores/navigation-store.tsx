// lib/stores/navigation-store.ts
import { create } from "zustand";

export interface NavigationState {
  isNavbarVisible: boolean;
  toggleNavbarVisibility: () => void;
  setNavbarVisibility: (visible: boolean) => void;
  resetStore: () => void;
}

export const useNavigationStore = create<NavigationState>((set) => ({
  isNavbarVisible: true,
  toggleNavbarVisibility: () => set((state) => ({ isNavbarVisible: !state.isNavbarVisible })),
  setNavbarVisibility: (visible) => set({ isNavbarVisible: visible }),
  resetStore: () => set({ isNavbarVisible: true }),
}));