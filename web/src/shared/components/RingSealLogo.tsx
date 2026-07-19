interface RingSealLogoProps {
  size?: number;
  variant?: "light" | "dark";
}

export function RingSealLogo({
  size = 27,
  variant = "dark",
}: RingSealLogoProps) {
  const isLight = variant === "light";
  const ringColor = isLight ? "rgba(255,255,255,0.85)" : "var(--frost-deep)";
  const innerRingColor = isLight
    ? "rgba(255,255,255,0.35)"
    : "var(--line-strong)";
  const textColor = isLight ? "#fff" : "var(--frost-deep)";

  return (
    <div
      className="relative inline-flex flex-shrink-0 items-center justify-center rounded-full border"
      style={{ width: size, height: size, borderColor: ringColor }}
    >
      <span
        className="font-display font-medium"
        style={{ fontSize: size * 0.48, color: textColor, lineHeight: 1 }}
      >
        M
      </span>
      <span
        className="absolute inset-[3px] rounded-full border"
        style={{ borderColor: innerRingColor }}
      />
    </div>
  );
}
