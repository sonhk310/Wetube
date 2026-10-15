"use client";

import {useChat, useDataChannel} from "@livekit/components-react";
import {Button, Flex, Tooltip} from "@radix-ui/themes";
import {useState} from "react";

export function ReactionBar() {
    const [encoder] = useState(() => new TextEncoder());
    const {send} = useDataChannel("reactions");
    const [lastReaction, setLastReaction] = useState("");

    const onSend = (emoji: string) => {
        // send(encoder.encode(emoji), false as any);
        send(encoder.encode(emoji), {reliable: false});
        setLastReaction(emoji);
    };

    return (
        <Flex
            gap="2"
            justify="center"
            align="center"
            className="border-t border-accent-5 bg-accent-3 h-[100px] text-center"
        >
            <Tooltip content="Fire" delayDuration={0}>
                <Button size="4" variant="outline" onClick={() => onSend("🔥")}>
                    🔥
                </Button>
            </Tooltip>
            <Tooltip content="Applause">
                <Button size="4" variant="outline" onClick={() => onSend("👏")}>
                    👏
                </Button>
            </Tooltip>
            <Tooltip content="LOL">
                <Button size="4" variant="outline" onClick={() => onSend("🤣")}>
                    🤣
                </Button>
            </Tooltip>
            <Tooltip content="Love">
                <Button size="4" variant="outline" onClick={() => onSend("❤️")}>
                    ❤️
                </Button>
            </Tooltip>
            <Tooltip content="Confetti">
                <Button size="4" variant="outline" onClick={() => onSend("🎉")}>
                    🎉
                </Button>
            </Tooltip>
        </Flex>
    );
}
