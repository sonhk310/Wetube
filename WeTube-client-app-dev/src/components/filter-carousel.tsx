"use client";

import { Carousel, CarouselApi, CarouselContent, CarouselItem, CarouselNext, CarouselPrevious } from "@/components/ui/carousel";
import { Badge } from "./ui/badge";
import { cn } from "@/lib/utils";
import { useEffect, useState } from "react";
import { Skeleton } from "./ui/skeleton";

interface FilterCarouselProps {
    value?: string | null;
    isLoading?: boolean;
    onSelect: (value: string | null) => void;
    data: {
        value: string;
        label: string;
    }[];
}
export const FilterCarousel = ({
    value,
    isLoading,
    onSelect,
    data,
}: FilterCarouselProps) => {
    const [api, setApi] = useState<CarouselApi>();
    // Còn cuộn được sang trái / phải không: chỉ hiện mũi tên và dải mờ ở phía còn cuộn được
    const [canPrev, setCanPrev] = useState(false);
    const [canNext, setCanNext] = useState(false);

    useEffect(() => {
        if (!api) return;
        const update = () => {
            setCanPrev(api.canScrollPrev());
            setCanNext(api.canScrollNext());
        };
        update();
        api.on("select", update).on("reInit", update).on("scroll", update);
        return () => {
            api.off("select", update).off("reInit", update).off("scroll", update);
        };
    }, [api]);

    return (
        <Carousel
            setApi={setApi}
            opts={{
                align: "start",
                dragFree: true,
            }}
            className="w-full"
        >
            <CarouselContent className="-ml-3">
                {!isLoading && <CarouselItem onClick={() => onSelect?.(null)} className="pl-3 basis-auto">
                    <Badge
                        variant={!value ? "default" : "secondary"}
                        className="rounded-lg px-3 py-1 cursor-pointer whitespace-nowrap text-sm"
                    >
                        Tất cả
                    </Badge>
                </CarouselItem>}
                {isLoading &&
                    Array.from({ length: 14 }).map((_, index) => (
                        <CarouselItem key={index} className="pl-3 basis-auto">
                            <Skeleton className="rounded-lg px-3 py-1 h-full text-sm w-[100px] font-semibold">
                                &nbsp;
                            </Skeleton>
                        </CarouselItem>
                    ))
                }
                {!isLoading && data.map((item) => (
                    <CarouselItem key={item.value} className="pl-3 basis-auto" onClick={() => onSelect?.(item.value)}>
                        <Badge variant={value === item.value ? "default" : "secondary"}
                            className="rounded-lg px-3 py-1 cursor-pointer whitespace-nowrap text-sm"
                        >
                            {item.label}
                        </Badge>
                    </CarouselItem>
                ))}
            </CarouselContent>

            <div className={cn(
                "absolute inset-y-0 left-0 w-20 z-10 bg-gradient-to-r from-white via-white/90 to-transparent pointer-events-none",
                !canPrev && "hidden"
            )} />
            <div className={cn(
                "absolute inset-y-0 right-0 w-20 z-10 bg-gradient-to-l from-white via-white/90 to-transparent pointer-events-none",
                !canNext && "hidden"
            )} />
            <CarouselPrevious className="z-20 left-0 disabled:hidden" />
            <CarouselNext className="z-20 right-0 disabled:hidden" />
        </Carousel>
    )
} 