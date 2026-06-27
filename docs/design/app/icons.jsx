/* Icon set — Feather-style line icons. <Icon name="..." size={18} /> */
const ICONS = {
  // nav
  grid: 'M3 3h7v7H3zM14 3h7v7h-7zM14 14h7v7h-7zM3 14h7v7H3z',
  wallet: 'M3 7h15a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7zM3 7l0-1a2 2 0 0 1 2-2h11M17 12.5h.01',
  card: 'M2 6h20v12H2zM2 10h20',
  target: 'M12 12m-9 0a9 9 0 1 0 18 0a9 9 0 1 0-18 0M12 12m-5 0a5 5 0 1 0 10 0a5 5 0 1 0-10 0M12 12m-1 0a1 1 0 1 0 2 0a1 1 0 1 0-2 0',
  trending: 'M3 17l6-6 4 4 8-8M15 7h6v6',
  repeat: 'M17 2l4 4-4 4M3 11V9a4 4 0 0 1 4-4h14M7 22l-4-4 4-4M21 13v2a4 4 0 0 1-4 4H3',
  // category / misc
  cart: 'M6 6h15l-1.5 9h-12zM6 6L5 2H2M9 20a1 1 0 1 0 0 .01M18 20a1 1 0 1 0 0 .01',
  utensils: 'M4 3v7a2 2 0 0 0 2 2h0a2 2 0 0 0 2-2V3M6 12v9M18 3c-1.5 0-3 1.5-3 5s1.5 4 3 4v9',
  bag: 'M6 7h12l-1 13H7zM9 7a3 3 0 0 1 6 0',
  home: 'M3 11l9-7 9 7M5 10v10h14V10',
  bolt: 'M13 2L4 14h7l-1 8 9-12h-7z',
  car: 'M5 13l1.5-5h11L19 13M5 13h14v5H5zM5 13v0M7.5 18v2M16.5 18v2M7 16h.01M17 16h.01',
  fuel: 'M3 21V5a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v16M3 13h10M16 7l3 3v7a2 2 0 0 1-4 0',
  heart: 'M20.8 6.6a5 5 0 0 0-7.1 0L12 8.3l-1.7-1.7a5 5 0 0 0-7.1 7.1l1.7 1.7L12 22l7.1-6.6 1.7-1.7a5 5 0 0 0 0-7.1z',
  book: 'M4 4v16a2 2 0 0 1 2-2h14V2H6a2 2 0 0 0-2 2z',
  sparkle: 'M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8zM19 3v3M20.5 4.5h-3',
  shirt: 'M16 3l4 2-2 4-2-1v13H8V8L6 9 4 5l4-2 4 2z',
  play: 'M8 5v14l11-7z',
  swap: 'M7 10l-4 4 4 4M3 14h12M17 14l4-4-4-4M21 10H9',
  'arrow-down-left': 'M17 7L7 17M17 17H7V7',
  dots: 'M5 12h.01M12 12h.01M19 12h.01',
  shield: 'M12 2l8 3v6c0 5-3.5 8.5-8 11-4.5-2.5-8-6-8-11V5z',
  plane: 'M21 16v-2l-8-5V3.5a1.5 1.5 0 0 0-3 0V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5z',
  laptop: 'M4 6h16v10H4zM2 20h20M9 16h6',
  // UI
  search: 'M11 11m-7 0a7 7 0 1 0 14 0a7 7 0 1 0-14 0M21 21l-4.3-4.3',
  bell: 'M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0',
  plus: 'M12 5v14M5 12h14',
  chevDown: 'M6 9l6 6 6-6',
  chevRight: 'M9 6l6 6-6 6',
  chevLeft: 'M15 6l-6 6 6 6',
  arrowUp: 'M12 19V5M5 12l7-7 7 7',
  arrowDownRight: 'M7 7l10 10M17 7v10H7',
  arrowUpRight: 'M7 17L17 7M7 7h10v10',
  filter: 'M3 5h18l-7 8v6l-4 2v-8z',
  settings: 'M12 12m-3 0a3 3 0 1 0 6 0a3 3 0 1 0-6 0M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-2.7 1.1V21a2 2 0 1 1-4 0v-.1A1.6 1.6 0 0 0 7 19.4a1.6 1.6 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0-1.1-2.7H1a2 2 0 1 1 0-4h.1A1.6 1.6 0 0 0 2.6 7a1.6 1.6 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.8.3H7a1.6 1.6 0 0 0 1-1.5V1a2 2 0 1 1 4 0v.1a1.6 1.6 0 0 0 1 1.5 1.6 1.6 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8V7a1.6 1.6 0 0 0 1.5 1H23a2 2 0 1 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1z',
  download: 'M12 3v12M7 10l5 5 5-5M5 21h14',
  upload: 'M12 21V9M7 14l5-5 5 5M5 3h14',
  check: 'M5 12l5 5 9-11',
  pix: 'M12 2l4 4-4 4-4-4zM2 12l4-4 4 4-4 4zM22 12l-4-4-4 4 4 4zM12 14l4 4-4 4-4-4z',
  calendar: 'M5 5h14v15H5zM5 9h14M9 3v4M15 3v4',
  clock: 'M12 12m-9 0a9 9 0 1 0 18 0a9 9 0 1 0-18 0M12 7v5l3 2',
  flame: 'M12 2c2 4 5 5 5 9a5 5 0 0 1-10 0c0-1.5.5-2.5 1-3 .3 1 1 1.5 1.5 1.5C9 8 10 5 12 2z',
  eye: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7zM12 12m-3 0a3 3 0 1 0 6 0a3 3 0 1 0-6 0',
  logout: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9',
  building: 'M4 21V5a2 2 0 0 1 2-2h7a2 2 0 0 1 2 2v16M15 21V9h3a2 2 0 0 1 2 2v10M8 7h3M8 11h3M8 15h3',
  banknote: 'M2 6h20v12H2zM12 12m-2.5 0a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0M6 9h.01M18 15h.01',
  menu: 'M3 6h18M3 12h18M3 18h18',
  x: 'M6 6l12 12M18 6L6 18',
  // auth + errors
  mail: 'M3 6h18v12H3zM3 7l9 6 9-6',
  lock: 'M6 11V8a6 6 0 0 1 12 0v3M5 11h14v9H5zM12 15v2',
  user: 'M5 21v-1a7 7 0 0 1 14 0v1M12 7m-4 0a4 4 0 1 0 8 0a4 4 0 1 0-8 0',
  eyeOff: 'M3 3l18 18M10.6 10.6a3 3 0 0 0 4 4M9.4 5.2A9.5 9.5 0 0 1 12 5c6 0 10 7 10 7a17 17 0 0 1-3 3.6M6.3 6.3A17 17 0 0 0 2 12s4 7 10 7a9.4 9.4 0 0 0 3.6-.7',
  alertTriangle: 'M12 3.2L21.5 20H2.5zM12 10v4M12 17.5h.01',
  alertCircle: 'M12 12m-9 0a9 9 0 1 0 18 0a9 9 0 1 0-18 0M12 8v4M12 16h.01',
  compass: 'M12 12m-9 0a9 9 0 1 0 18 0a9 9 0 1 0-18 0M16.2 7.8l-2.3 6.4-6.4 2.3 2.3-6.4z',
  arrowLeft: 'M19 12H5M12 19l-7-7 7-7',
  lockKey: 'M5 11h14v9H5zM6 11V8a6 6 0 0 1 12 0v3M12 14.5m-1.5 0a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0M12 16v2.5',
};

function Icon({ name, size = 18, stroke = 2, className = '', style = {}, fill = false }) {
  const d = ICONS[name];
  if (!d) return null;
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none"
      stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round"
      className={className} style={style} aria-hidden="true">
      <path d={d} fill={fill ? 'currentColor' : 'none'} stroke={fill ? 'none' : 'currentColor'} />
    </svg>
  );
}

window.Icon = Icon;
window.ICONS = ICONS;
