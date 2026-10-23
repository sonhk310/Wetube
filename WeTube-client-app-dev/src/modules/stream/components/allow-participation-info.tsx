"use client";

import { InfoCircledIcon } from "@radix-ui/react-icons";
import {
  AccessibleIcon,
  IconButton,
  Popover,
  Strong,
  Text,
} from "@radix-ui/themes";

export function AllowParticipationInfo() {
  return (
    <Popover.Root>
      <Popover.Trigger>
        <IconButton size="1" variant="ghost" color="gray">
          <AccessibleIcon label="Tìm hiểu thêm về tuỳ chọn nền của khung hình">
            <InfoCircledIcon />
          </AccessibleIcon>
        </IconButton>
      </Popover.Trigger>

      <Popover.Content
        size="1"
        style={{ maxWidth: 360 }}
        side="top"
        align="center"
      >
        <Text as="p" size="1">
          Khi bật, người xem có thể <Strong>giơ tay</Strong>. Nếu được
          chủ phòng chấp nhận, họ có thể chia sẻ âm thanh và hình ảnh. Chủ phòng
          cũng có thể <Strong>mời</Strong> người xem chia sẻ âm thanh và
          hình ảnh.
        </Text>
      </Popover.Content>
    </Popover.Root>
  );
}
