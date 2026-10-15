"use client";

import {useEffect, useRef} from "react";

export default function NeonLinesBackground() {
    const canvasRef = useRef<HTMLCanvasElement>(null);
    // let opacity = 0;


    useEffect(() => {
        const canvas = canvasRef.current!;
        const ctx = canvas.getContext("2d")!;

        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;

        const colors = ["#ff4ecd", "#4deaff", "#a855f7", "#22d3ee", "#f472b6"];
        const lines: {
            x: number;
            y: number;
            width: number;
            speed: number;
            color: string;
            delay: number;
        }[] = [];

        function createLine() {
            const y = Math.random() * canvas.height;
            const width = 100 + Math.random() * 300;
            const speed = 2 + Math.random() * 4;
            const color = colors[Math.floor(Math.random() * colors.length)];
            const delay = Math.random() * 2000;
            lines.push({ x: -width, y, width, speed, color, delay });
        }
        setInterval(createLine, 400);

        const text = "Welcome";
        ctx.font = "90px 'Pacifico', cursive";
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";

        const dashLen = 220;
        let offset = dashLen;
        const speed = 1;

        function draw() {
            ctx.fillStyle = "rgba(10, 0, 25, 0.3)";
            ctx.fillRect(0, 0, canvas.width, canvas.height);

            lines.forEach((line) => {
                if (line.delay > 0) {
                    line.delay -= 16;
                    return;
                }

                ctx.shadowBlur = 20;
                ctx.shadowColor = line.color;
                ctx.strokeStyle = line.color;
                ctx.lineWidth = 3;
                ctx.beginPath();
                ctx.moveTo(line.x, line.y);
                ctx.lineTo(line.x + line.width, line.y);
                ctx.stroke();

                line.x += line.speed;
            });

            for (let i = lines.length - 1; i >= 0; i--) {
                if (lines[i].x > canvas.width + lines[i].width) {
                    lines.splice(i, 1);
                }
            }

            const gradient = ctx.createLinearGradient(
                canvas.width / 2 - 200,
                canvas.height / 2,
                canvas.width / 2 + 200,
                canvas.height / 2
            );
            gradient.addColorStop(0, "#ff4ecd");
            gradient.addColorStop(0.5, "#4deaff");
            gradient.addColorStop(1, "#a855f7");

            ctx.save();
            ctx.lineWidth = 2;
            ctx.strokeStyle = gradient;
            ctx.shadowBlur = 25;
            ctx.shadowColor = "#ff4ecd";

            ctx.setLineDash([dashLen - offset, offset - speed]);
            ctx.lineDashOffset = -offset;

            ctx.strokeText(text, canvas.width / 2, canvas.height / 2);
            ctx.restore();

            if (offset > 0) {
                offset -= speed * 0.5;
            } else {
                ctx.save();
                ctx.fillStyle = gradient;
                ctx.shadowBlur = 30;
                ctx.shadowColor = "#ff4ecd";
                ctx.fillText(text, canvas.width / 2, canvas.height / 2);
                ctx.restore();
            }

            requestAnimationFrame(draw);
        }

        draw();

        const handleResize = () => {
            canvas.width = window.innerWidth;
            canvas.height = window.innerHeight;
        };
        window.addEventListener("resize", handleResize);
        return () => window.removeEventListener("resize", handleResize);
    }, []);

    return (
        <canvas
            ref={canvasRef}
            className="absolute inset-0 w-full h-full pointer-events-none -z-10"
        />
    );
}
