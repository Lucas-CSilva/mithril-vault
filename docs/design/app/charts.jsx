/* Charts — hand-built SVG. Exported to window. */

// Catmull-Rom → cubic bezier smoothing
function smoothPath(pts) {
  if (pts.length < 2) return '';
  let d = `M ${pts[0][0]},${pts[0][1]}`;
  for (let i = 0; i < pts.length - 1; i++) {
    const p0 = pts[i - 1] || pts[i];
    const p1 = pts[i];
    const p2 = pts[i + 1];
    const p3 = pts[i + 2] || p2;
    const t = 0.16;
    const c1x = p1[0] + (p2[0] - p0[0]) * t;
    const c1y = p1[1] + (p2[1] - p0[1]) * t;
    const c2x = p2[0] - (p3[0] - p1[0]) * t;
    const c2y = p2[1] - (p3[1] - p1[1]) * t;
    d += ` C ${c1x},${c1y} ${c2x},${c2y} ${p2[0]},${p2[1]}`;
  }
  return d;
}

/* ---- Spline area chart ------------------------------------- */
function Spline({ data, height = 220, color = 'var(--frost-deep)', fill = 'rgba(94,129,172,0.12)', labels = null, fmt = (v)=>v, id = 'sp' }) {
  const ref = React.useRef(null);
  const [w, setW] = React.useState(640);
  const [hover, setHover] = React.useState(null);
  React.useEffect(() => {
    const el = ref.current; if (!el) return;
    const ro = new ResizeObserver(() => setW(el.clientWidth));
    ro.observe(el); setW(el.clientWidth);
    return () => ro.disconnect();
  }, []);

  const padL = 8, padR = 8, padT = 18, padB = labels ? 26 : 12;
  const min = Math.min(...data), max = Math.max(...data);
  const range = (max - min) || 1;
  const innerW = w - padL - padR, innerH = height - padT - padB;
  const pts = data.map((v, i) => [
    padL + (innerW * i) / (data.length - 1),
    padT + innerH - ((v - min) / range) * innerH,
  ]);
  const line = smoothPath(pts);
  const area = `${line} L ${pts[pts.length-1][0]},${padT+innerH} L ${pts[0][0]},${padT+innerH} Z`;

  const onMove = (e) => {
    const rect = ref.current.getBoundingClientRect();
    const x = e.clientX - rect.left;
    let idx = Math.round(((x - padL) / innerW) * (data.length - 1));
    idx = Math.max(0, Math.min(data.length - 1, idx));
    setHover(idx);
  };

  const gradId = `grad-${id}`;
  return (
    <div ref={ref} style={{ width: '100%', position: 'relative' }}
      onMouseMove={onMove} onMouseLeave={() => setHover(null)}>
      <svg width={w} height={height} style={{ display: 'block', overflow: 'visible' }}>
        <defs>
          <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={color} stopOpacity="0.18" />
            <stop offset="100%" stopColor={color} stopOpacity="0" />
          </linearGradient>
        </defs>
        {/* subtle gridlines */}
        {[0.25,0.5,0.75].map((g,i)=>(
          <line key={i} x1={padL} x2={w-padR} y1={padT+innerH*g} y2={padT+innerH*g}
            stroke="var(--line)" strokeWidth="1" strokeDasharray="2 5" />
        ))}
        <path d={area} fill={`url(#${gradId})`} />
        <path d={line} fill="none" stroke={color} strokeWidth="2.4" strokeLinecap="round" />
        {/* end dot */}
        <circle cx={pts[pts.length-1][0]} cy={pts[pts.length-1][1]} r="4" fill={color} />
        <circle cx={pts[pts.length-1][0]} cy={pts[pts.length-1][1]} r="8" fill={color} opacity="0.14" />
        {hover != null && (
          <g>
            <line x1={pts[hover][0]} x2={pts[hover][0]} y1={padT} y2={padT+innerH}
              stroke="var(--line-strong)" strokeWidth="1" />
            <circle cx={pts[hover][0]} cy={pts[hover][1]} r="5" fill="var(--surface)" stroke={color} strokeWidth="2.4" />
          </g>
        )}
        {labels && labels.map((l, i) => (
          (i % Math.ceil(labels.length/6) === 0 || i === labels.length-1) &&
          <text key={i} x={pts[i][0]} y={height-6} textAnchor="middle"
            fontFamily="var(--mono)" fontSize="10" fill="var(--ink-4)">{l}</text>
        ))}
      </svg>
      {hover != null && (
        <div style={{
          position: 'absolute', left: Math.min(Math.max(pts[hover][0]-60, 0), w-120),
          top: 0, pointerEvents: 'none',
          background: 'var(--ink)', color: '#fff', padding: '7px 10px', borderRadius: 8,
          boxShadow: 'var(--sh-md)', minWidth: 110,
        }}>
          {labels && <div style={{ fontFamily:'var(--mono)', fontSize:9.5, opacity:.6, letterSpacing:'.05em' }}>{labels[hover]}</div>}
          <div style={{ fontFamily:'var(--mono)', fontSize:13, fontWeight:600 }}>{fmt(data[hover])}</div>
        </div>
      )}
    </div>
  );
}

/* ---- Donut ------------------------------------------------- */
function Donut({ data, size = 200, thickness = 26, gap = 0.018, active = null, onSlice = null }) {
  const total = data.reduce((s, d) => s + d.value, 0) || 1;
  const r = (size - thickness) / 2 - 2;
  const cx = size / 2, cy = size / 2;
  let a = -Math.PI / 2;
  const arcs = data.map((d) => {
    const frac = d.value / total;
    const a0 = a + gap * Math.PI;
    const a1 = a + frac * 2 * Math.PI - gap * Math.PI;
    a += frac * 2 * Math.PI;
    const large = (a1 - a0) > Math.PI ? 1 : 0;
    const x0 = cx + r * Math.cos(a0), y0 = cy + r * Math.sin(a0);
    const x1 = cx + r * Math.cos(a1), y1 = cy + r * Math.sin(a1);
    return { d, path: `M ${x0},${y0} A ${r},${r} 0 ${large} 1 ${x1},${y1}`, frac };
  });
  return (
    <svg width={size} height={size} style={{ display: 'block' }}>
      {arcs.map((s, i) => {
        const isActive = active === null || active === s.d.id;
        return (
          <path key={i} d={s.path} fill="none" stroke={s.d.color}
            strokeWidth={active === s.d.id ? thickness + 5 : thickness}
            strokeLinecap="round"
            opacity={isActive ? 1 : 0.28}
            style={{ cursor: onSlice ? 'pointer' : 'default', transition: 'all .25s' }}
            onMouseEnter={() => onSlice && onSlice(s.d.id)}
            onMouseLeave={() => onSlice && onSlice(null)} />
        );
      })}
    </svg>
  );
}

/* ---- Progress bar ------------------------------------------ */
function Progress({ value, max, height = 8, threshold = true, color = 'var(--frost-deep)', track = 'var(--surface-3)' }) {
  const pct = Math.min(100, (value / (max || 1)) * 100);
  let c = color;
  if (threshold) {
    if (pct >= 100) c = 'var(--neg)';
    else if (pct >= 80) c = 'var(--warn)';
    else c = 'var(--pos)';
  }
  return (
    <div style={{ height, background: track, borderRadius: 99, overflow: 'hidden', width: '100%' }}>
      <div style={{ width: `${Math.min(100,pct)}%`, height: '100%', background: c, borderRadius: 99, transition: 'width .6s cubic-bezier(.22,.61,.36,1)' }} />
    </div>
  );
}

/* ---- Ring (single value gauge) ----------------------------- */
function Ring({ value, max, size = 120, thickness = 10, color = 'var(--frost-deep)', children }) {
  const r = (size - thickness) / 2;
  const c = 2 * Math.PI * r;
  const pct = Math.min(1, value / (max || 1));
  return (
    <div style={{ position: 'relative', width: size, height: size }}>
      <svg width={size} height={size} style={{ transform: 'rotate(-90deg)' }}>
        <circle cx={size/2} cy={size/2} r={r} fill="none" stroke="var(--surface-3)" strokeWidth={thickness} />
        <circle cx={size/2} cy={size/2} r={r} fill="none" stroke={color} strokeWidth={thickness}
          strokeLinecap="round" strokeDasharray={c} strokeDashoffset={c * (1 - pct)}
          style={{ transition: 'stroke-dashoffset .8s cubic-bezier(.22,.61,.36,1)' }} />
      </svg>
      <div style={{ position: 'absolute', inset: 0, display: 'grid', placeItems: 'center' }}>{children}</div>
    </div>
  );
}

/* ---- Mini sparkline ---------------------------------------- */
function Sparkline({ data, width = 84, height = 30, color = 'var(--frost-deep)' }) {
  const min = Math.min(...data), max = Math.max(...data), range = (max-min)||1;
  const pts = data.map((v,i) => [ (width*i)/(data.length-1), height - 3 - ((v-min)/range)*(height-6) ]);
  return (
    <svg width={width} height={height} style={{ display:'block', overflow:'visible' }}>
      <path d={smoothPath(pts)} fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" />
      <circle cx={pts[pts.length-1][0]} cy={pts[pts.length-1][1]} r="2.5" fill={color} />
    </svg>
  );
}

/* ---- Stacked horizontal bar (allocation) ------------------- */
function StackBar({ data, height = 10 }) {
  const total = data.reduce((s,d)=>s+d.value,0)||1;
  return (
    <div style={{ display:'flex', gap: 3, height, width:'100%' }}>
      {data.map((d,i)=>(
        <div key={i} title={d.label} style={{ width:`${(d.value/total)*100}%`, background:d.color, borderRadius: 3, minWidth: 2 }} />
      ))}
    </div>
  );
}

Object.assign(window, { Spline, Donut, Progress, Ring, Sparkline, StackBar, smoothPath });
