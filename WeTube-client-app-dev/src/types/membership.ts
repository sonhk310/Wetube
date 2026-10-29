/** Cấp hội viên trả phí của một kênh, như người xem thấy. Giá tính bằng VND cho 30 ngày. */
export interface MemberTier {
  id: string;
  title: string;
  price: number;
  description: string | null;
}

/** Hội viên hiện tại của người xem ở một kênh. until: ngày hết hạn (yyyy-MM-dd, hết ngày này). */
export interface MyMembership {
  tierId: string;
  tierTitle: string;
  until: string;
}

export interface ChannelTiers {
  tiers: MemberTier[];
  membership: MyMembership | null;
}

export interface OwnerTier extends MemberTier {
  archived: boolean;
  memberCount: number;
}

export interface MemberRow {
  userId: string;
  name: string;
  picture: string | null;
  handle: string | null;
  tierTitle: string;
  since: string;
  until: string;
}

export interface MembershipRevenue {
  total: number;
  wetubeFee: number;
  channelRevenue: number;
}

export interface OwnerMembers {
  tiers: OwnerTier[];
  members: MemberRow[];
  revenue: MembershipRevenue;
}

export interface TierInput {
  title: string;
  price: number;
  description: string;
}

/** Luật cấp hội viên, khớp với backend (MemberTierServiceImpl). */
export const MAX_PAID_TIERS = 3;
export const MIN_TIER_PRICE = 10_000;
export const MAX_TIER_PRICE = 500_000;
export const MEMBERSHIP_DAYS = 30;
export const WETUBE_FEE_PERCENT = 10;
