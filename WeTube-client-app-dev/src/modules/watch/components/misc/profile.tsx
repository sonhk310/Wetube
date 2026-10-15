import Image from "next/image";

export default function Profile({
                                    url,
                                    size,
                                }: {
    url?: string;
    size?: string;
}) {
    if (!url) {
        return <div className={`rounded-full h-${size || "10"} w-${size || "10"} bg-gray-300`}/>;
    }

    return (
        <Image
            className={`rounded-full h-${size || "10"} w-${size || "10"} cursor-pointer`}
            src={url}
            alt="user profile"
            height={40}
            width={40}
        />
    );
}
