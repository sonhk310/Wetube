import jwt from "jsonwebtoken";
import {
  AccessToken,
  CreateIngressOptions,
  IngressClient,
  IngressInfo,
  IngressInput,
  ParticipantInfo,
  ParticipantPermission,
  RoomServiceClient,
} from "livekit-server-sdk";
import {
  TrackSource,
  IngressVideoEncodingPreset,
  IngressAudioEncodingPreset,
  IngressAudioOptions,
  IngressVideoOptions,
} from "@livekit/protocol";

export type RoomMetadata = {
  creatorIdentity: string;
  enableChat: boolean;
  allowParticipation: boolean;
};

export type ParticipantMetadata = {
  hand_raised: boolean;
  invited_to_stage: boolean;
  avatar_image: string;
};

export type Config = {
  wsUrl: string;
  api_key: string;
  api_secret: string;
};

export type Session = {
  identity: string;
  roomName: string;
};

export type ConnectionDetails = {
  token: string;
  wsUrl: string;
};

export type CreateIngressParams = {
  roomName?: string;
  ingress_type: string;
  metadata: RoomMetadata;
};

export type CreateIngressResponse = {
  ingress: IngressInfo;
  authToken: string;
  connectionDetails: ConnectionDetails;
};

export type CreateStreamParams = {
  roomName?: string;
  metadata: RoomMetadata;
};

export type CreateStreamResponse = {
  authToken: string;
  connectionDetails: ConnectionDetails;
};

export type JoinStreamParams = {
  roomName: string;
  identity: string;
};

export type JoinStreamResponse = {
  authToken: string;
  connectionDetails: ConnectionDetails;
};

export type InviteToStageParams = {
  identity: string;
};

export type RemoveFromStageParams = {
  identity?: string;
};

export type ErrorResponse = {
  error: string;
};

export function getSessionFromReq(req: Request): Session {
  const authHeader = req.headers.get("authorization");
  const token = authHeader?.split(" ")[1];
  if (!token) {
    throw new Error("No authorization header found");
  }
  const verified = jwt.verify(token, process.env.LIVEKIT_API_SECRET!);
  if (!verified) {
    throw new Error("Invalid token");
  }
  const decoded = jwt.decode(token) as Session;
  return decoded;
}

export class Controller {
  private ingressService: IngressClient;
  private roomService: RoomServiceClient;

  constructor() {
    const httpUrl = process.env
      .LIVEKIT_wsUrl!.replace("wss://", "https://")
      .replace("ws://", "http://");
    this.ingressService = new IngressClient(httpUrl);
    this.roomService = new RoomServiceClient(
      httpUrl,
      process.env.LIVEKIT_API_KEY!,
      process.env.LIVEKIT_API_SECRET!
    );
  }

  async createIngress({
    metadata,
    roomName,
    ingress_type = "rtmp",
  }: CreateIngressParams): Promise<CreateIngressResponse> {
    if (!roomName) {
      roomName = generateRoomId();
    }

    // Create room and ingress

    await this.roomService.createRoom({
      name: roomName,
      metadata: JSON.stringify(metadata),
    });

    const options: CreateIngressOptions = {
      name: roomName,
      roomName: roomName,
      participantName: `${metadata.creatorIdentity} (via OBS)`,
      participantIdentity: `${metadata.creatorIdentity} (via OBS)`,

      video: new IngressVideoOptions({
        source: TrackSource.CAMERA,
        encodingOptions: {
          case: "preset",
          value: IngressVideoEncodingPreset.H264_1080P_30FPS_3_LAYERS,
        },
      }),

      // Sử dụng 'new' để tạo một instance của class IngressAudioOptions
      audio: new IngressAudioOptions({
        source: TrackSource.MICROPHONE,
        encodingOptions: {
          case: "preset",
          value: IngressAudioEncodingPreset.OPUS_STEREO_96KBPS,
        },
      }),
    };

    const ingress = await this.ingressService.createIngress(
      ingress_type === "whip"
        ? IngressInput.WHIP_INPUT
        : IngressInput.RTMP_INPUT,
      options
    );

    // Create viewer access token

    const at = new AccessToken(
      process.env.LIVEKIT_API_KEY!,
      process.env.LIVEKIT_API_SECRET!,
      {
        identity: metadata.creatorIdentity,
      }
    );

    at.addGrant({
      room: roomName,
      roomJoin: true,
      canPublish: false,
      canSubscribe: true,
      canPublishData: true,
    });

    const authToken = this.createAuthToken(roomName, metadata.creatorIdentity);

    return {
      ingress,
      authToken: authToken,
      connectionDetails: {
        wsUrl: process.env.LIVEKIT_wsUrl!,
        token: await at.toJwt(),
      },
    };
  }

  async createStream({
    metadata,
    roomName: roomName,
  }: CreateStreamParams): Promise<CreateStreamResponse> {
    const at = new AccessToken(
      process.env.LIVEKIT_API_KEY!,
      process.env.LIVEKIT_API_SECRET!,
      {
        identity: metadata.creatorIdentity,
      }
    );

    if (!roomName) {
      roomName = generateRoomId();
    }
    at.addGrant({
      room: roomName,
      roomJoin: true,
      canPublish: true,
      canSubscribe: true,
    });

    // TODO turn off auto creation in the dashboard
    await this.roomService.createRoom({
      name: roomName,
      metadata: JSON.stringify(metadata),
    });

    const connectionDetails = {
      wsUrl: process.env.LIVEKIT_wsUrl!,
      token: await at.toJwt(),
    };

    const authToken = this.createAuthToken(roomName, metadata.creatorIdentity);

    return {
      authToken: authToken,
      connectionDetails,
    };
  }

  async stopStream(session: Session) {
    const rooms = await this.roomService.listRooms([session.roomName]);

    if (rooms.length === 0) {
      throw new Error("Room does not exist");
    }

    const room = rooms[0];
    const creatorIdentity = (JSON.parse(room.metadata) as RoomMetadata)
      .creatorIdentity;

    if (creatorIdentity !== session.identity) {
      throw new Error("Only the creator can invite to stage");
    }

    await this.roomService.deleteRoom(session.roomName);
  }

  async joinStream({
    identity,
    roomName,
  }: JoinStreamParams): Promise<JoinStreamResponse> {
    // Check for existing participant with same identity
    let exists = false;
    try {
      await this.roomService.getParticipant(roomName, identity);
      exists = true;
    } catch {}

    if (exists) {
      throw new Error("Participant already exists");
    }

    const at = new AccessToken(
      process.env.LIVEKIT_API_KEY!,
      process.env.LIVEKIT_API_SECRET!,
      {
        identity,
      }
    );

    at.addGrant({
      room: roomName,
      roomJoin: true,
      canPublish: false,
      canSubscribe: true,
      canPublishData: true,
    });

    const authToken = this.createAuthToken(roomName, identity);

    return {
      authToken: authToken,
      connectionDetails: {
        wsUrl: process.env.LIVEKIT_wsUrl!,
        token: await at.toJwt(),
      },
    };
  }

  async inviteToStage(session: Session, { identity }: InviteToStageParams) {
    const rooms = await this.roomService.listRooms([session.roomName]);

    if (rooms.length === 0) {
      throw new Error("Room does not exist");
    }

    const room = rooms[0];
    const creatorIdentity = (JSON.parse(room.metadata) as RoomMetadata)
      .creatorIdentity;

    if (creatorIdentity !== session.identity) {
      throw new Error("Only the creator can invite to stage");
    }

    const participant = await this.roomService.getParticipant(
      session.roomName,
      identity
    );
    const permission = participant.permission || ({} as ParticipantPermission);

    const metadata = this.getOrCreateParticipantMetadata(participant);
    metadata.invited_to_stage = true;

    // If hand is raised and invited to stage, then we let the put them on stage
    if (metadata.hand_raised) {
      permission.canPublish = true;
    }

    await this.roomService.updateParticipant(
      session.roomName,
      identity,
      JSON.stringify(metadata),
      permission
    );
  }

  async removeFromStage(session: Session, { identity }: RemoveFromStageParams) {
    if (!identity) {
      // remove self if no identity specified
      identity = session.identity;
    }

    const rooms = await this.roomService.listRooms([session.roomName]);

    if (rooms.length === 0) {
      throw new Error("Room does not exist");
    }

    const room = rooms[0];
    const creatorIdentity = (JSON.parse(room.metadata) as RoomMetadata)
      .creatorIdentity;

    if (creatorIdentity !== session.identity && identity !== session.identity) {
      throw new Error(
        "Only the creator or the participant him self can remove from stage"
      );
    }

    const participant = await this.roomService.getParticipant(
      session.roomName,
      session.identity
    );

    const permission = participant.permission || ({} as ParticipantPermission);
    const metadata = this.getOrCreateParticipantMetadata(participant);

    // Reset everything and disallow them from publishing (this will un-publish them automatically)
    metadata.hand_raised = false;
    metadata.invited_to_stage = false;
    permission.canPublish = false;

    await this.roomService.updateParticipant(
      session.roomName,
      identity,
      JSON.stringify(metadata),
      permission
    );
  }

  async raiseHand(session: Session) {
    const participant = await this.roomService.getParticipant(
      session.roomName,
      session.identity
    );

    const permission = participant.permission || ({} as ParticipantPermission);
    const metadata = this.getOrCreateParticipantMetadata(participant);
    metadata.hand_raised = true;

    // If hand is raised and invited to stage, then we let the put them on stage
    if (metadata.invited_to_stage) {
      permission.canPublish = true;
    }

    await this.roomService.updateParticipant(
      session.roomName,
      session.identity,
      JSON.stringify(metadata),
      permission
    );
  }

  getOrCreateParticipantMetadata(
    participant: ParticipantInfo
  ): ParticipantMetadata {
    if (participant.metadata) {
      return JSON.parse(participant.metadata) as ParticipantMetadata;
    }
    return {
      hand_raised: false,
      invited_to_stage: false,
      avatar_image: `https://api.multiavatar.com/${participant.identity}.png`,
    };
  }

  createAuthToken(roomName: string, identity: string) {
    return jwt.sign(
      JSON.stringify({ roomName, identity }),
      process.env.LIVEKIT_API_SECRET!
    );
  }
}

function generateRoomId(): string {
  return `${randomString(4)}-${randomString(4)}`;
}

function randomString(length: number): string {
  let result = "";
  const characters = "abcdefghijklmnopqrstuvwxyz0123456789";
  const charactersLength = characters.length;
  for (let i = 0; i < length; i++) {
    result += characters.charAt(Math.floor(Math.random() * charactersLength));
  }
  return result;
}
