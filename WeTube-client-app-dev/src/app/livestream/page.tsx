import {HomeActions} from "@/modules/stream/components/home-actions";
import {Container, Flex, Text} from "@radix-ui/themes";
import Image from "next/image";
import Background from "@/modules/stream/background";

export default function Home() {
    return (
        <main className="flex min-h-screen flex-col items-center gap-12 p-10 sm:p-24">
            <Background/>
            <Container size="1">
                <Flex direction="column" align="center" gap="5">
                    <Image
                        src="/WeTube.svg"
                        alt="LiveKit"
                        width="240"
                        height="120"
                        className="invert dark:invert-0 mt-8 mb-2"
                    />
                    <Text as="p" align="center" className="text-white">
                        Welcome to the WeTube livestream. You can join or start
                        your own stream. Hosted on{" "}
                    </Text>
                    <HomeActions/>
                    {/*<Separator orientation="horizontal" size="4" className="my-2"/>*/}

                </Flex>
            </Container>
        </main>
    );
}
