/* Subscriptions — recurring economy */

const CYCLE_LABEL = { MONTHLY:'Mensal', BIMONTHLY:'Bimestral', QUARTERLY:'Trimestral', SEMIANNUAL:'Semestral', ANNUAL:'Anual' };
const CYCLE_MULT = { MONTHLY:1, BIMONTHLY:0.5, QUARTERLY:1/3, SEMIANNUAL:1/6, ANNUAL:1/12 };

function monthlyEquiv(s) { return Math.round(s.amount * CYCLE_MULT[s.cycle]); }

function SubCard({ s }) {
  const cat = MV.data.catMap[s.cat];
  const cancelled = s.status === 'CANCELLED';
  const days = s.next ? Math.round((new Date(s.next) - new Date('2026-06-07')) / 86400000) : null;
  const urg = days != null && days <= 2 ? 'var(--neg)' : days != null && days <= 5 ? 'var(--warn)' : 'var(--ink-4)';
  return (
    <div className="card" style={{ padding: '18px 20px', opacity: cancelled ? 0.6 : 1 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 14 }}>
        <span style={{ width: 40, height: 40, borderRadius: 11, background: s.color+'22', color: s.color, display: 'grid', placeItems: 'center', fontWeight: 700, fontSize: 15, flexShrink: 0 }}>
          {s.name.slice(0,1)}
        </span>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 14, fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{s.name}</div>
          <div style={{ fontSize: 11.5, color: 'var(--ink-4)' }}>{CYCLE_LABEL[s.cycle]} · {cat.name}</div>
        </div>
        {cancelled && <span className="pill" style={{ background: 'var(--surface-3)', color: 'var(--ink-4)' }}>Cancelada</span>}
      </div>
      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
        <div>
          <span className="num" style={{ fontSize: 19, fontWeight: 700 }}>{MV.fmtBRL(s.amount)}</span>
          {s.cycle !== 'MONTHLY' && <span className="mono" style={{ fontSize: 10.5, color: 'var(--ink-4)', display: 'block', marginTop: 2 }}>≈ {MV.fmtBRL(monthlyEquiv(s))}/mês</span>}
        </div>
        {!cancelled && s.next && (
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: 11, color: 'var(--ink-4)' }}>próx. cobrança</div>
            <div className="mono" style={{ fontSize: 12, fontWeight: 700, color: urg }}>{MV.fmtDateShort(s.next)}</div>
          </div>
        )}
      </div>
    </div>
  );
}

function RenewalTimeline({ subs }) {
  const today = new Date('2026-06-07');
  const upcoming = subs
    .filter(s => s.status === 'ACTIVE' && s.next)
    .map(s => ({ ...s, days: Math.round((new Date(s.next) - today) / 86400000) }))
    .filter(s => s.days >= 0 && s.days <= 30)
    .sort((a,b) => a.days - b.days);
  const maxDays = 30;
  return (
    <div className="card" style={{ padding: '22px 24px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 28 }}>
        <h3 style={{ fontSize: 15, fontWeight: 700 }}>Calendário de Renovações</h3>
        <span className="mono" style={{ fontSize: 10.5, color: 'var(--ink-4)' }}>PRÓXIMOS 30 DIAS</span>
      </div>
      <div style={{ position: 'relative', height: 100, margin: '0 8px' }}>
        {/* track */}
        <div style={{ position: 'absolute', left: 0, right: 0, top: 6, height: 2, background: 'var(--line)', borderRadius: 2 }} />
        {/* today marker */}
        <div style={{ position: 'absolute', left: 0, top: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', transform: 'translateX(-50%)' }}>
          <div style={{ width: 14, height: 14, borderRadius: 99, background: 'var(--frost-deep)', border: '2.5px solid var(--surface)', boxShadow: '0 0 0 2px var(--frost-deep)' }} />
          <div className="mono" style={{ fontSize: 9, color: 'var(--frost-deep)', fontWeight: 700, marginTop: 4, whiteSpace: 'nowrap' }}>HOJE</div>
        </div>
        {/* events */}
        {upcoming.map((s, idx) => {
          const pct = (s.days / maxDays) * 100;
          const urg = s.days <= 2 ? 'var(--neg)' : s.days <= 5 ? 'var(--warn)' : 'var(--frost-soft)';
          const top = idx % 2 === 0 ? 22 : 52; // stagger to avoid overlap
          return (
            <div key={s.id} style={{ position: 'absolute', left: `${pct}%`, top: 0, transform: 'translateX(-50%)', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <div style={{ width: 10, height: 10, borderRadius: 99, background: urg, border: '2px solid var(--surface)', marginTop: 2 }} />
              <div style={{ position: 'absolute', top: `${top}px`, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
                <div style={{ width: 28, height: 28, borderRadius: 8, background: s.color + '22', color: s.color, display: 'grid', placeItems: 'center', fontWeight: 700, fontSize: 11 }}>
                  {s.name.slice(0, 1)}
                </div>
                <div style={{ fontSize: 10, fontWeight: 600, color: 'var(--ink-2)', whiteSpace: 'nowrap', maxWidth: 60, overflow: 'hidden', textOverflow: 'ellipsis', textAlign: 'center' }}>{s.name}</div>
                <div className="num" style={{ fontSize: 10, color: urg, fontWeight: 700 }}>{MV.fmtBRL(s.amount, {cents:false})}</div>
              </div>
            </div>
          );
        })}
        {/* end marker */}
        <div style={{ position: 'absolute', right: -4, top: 0 }}>
          <div style={{ width: 14, height: 14, borderRadius: 99, background: 'var(--surface-3)', border: '2px solid var(--line-strong)' }} />
          <div className="mono" style={{ fontSize: 9, color: 'var(--ink-4)', marginTop: 4, marginLeft: -8 }}>+30d</div>
        </div>
      </div>
    </div>
  );
}

function Subscriptions() {
  const [showAll, setShowAll] = React.useState(false);
  const all = MV.data.subscriptions;
  const active = all.filter(s => s.status === 'ACTIVE');
  const shown = showAll ? all : active;
  const monthlyTotal = active.reduce((s,x)=>s+monthlyEquiv(x),0);
  const annualTotal = monthlyTotal * 12;
  const biggest = active.reduce((m,x)=> monthlyEquiv(x) > monthlyEquiv(m) ? x : m, active[0]);

  // cost by category
  const byCat = {};
  active.forEach(s => { const c = MV.data.catMap[s.cat]; const top = c.parentId ? MV.data.catMap[c.parentId] : c; byCat[top.id] = byCat[top.id] || { label: top.name, value: 0, color: top.color }; byCat[top.id].value += monthlyEquiv(s); });
  const catArr = Object.values(byCat).sort((a,b)=>b.value-a.value);

  const stats = [
    { label: 'Custo mensal', value: monthlyTotal, icon: 'repeat', color: 'var(--frost-deep)' },
    { label: 'Custo anual', value: annualTotal, icon: 'calendar', color: 'var(--frost-pine)' },
    { label: 'Maior assinatura', value: monthlyEquiv(biggest), sub: biggest.name, icon: 'flame', color: 'var(--warn)' },
    { label: 'Assinaturas ativas', count: active.length, icon: 'check', color: 'var(--pos)' },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      {/* stats */}
      <div className="fade-in" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: 14 }}>
        {stats.map((st,i)=>(
          <div key={i} className="card" style={{ padding: '18px 20px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
              <span style={{ width: 32, height: 32, borderRadius: 9, background: st.color+'1f', color: st.color, display: 'grid', placeItems: 'center' }}><Icon name={st.icon} size={15} /></span>
              <span style={{ fontSize: 12.5, color: 'var(--ink-3)' }}>{st.label}</span>
            </div>
            <div className="num" style={{ fontSize: 23, fontWeight: 700, letterSpacing: '-.02em' }}>
              {st.count != null ? st.count : MV.fmtBRL(st.value)}
            </div>
            {st.sub && <div style={{ fontSize: 11.5, color: 'var(--ink-4)', marginTop: 3 }}>{st.sub}</div>}
          </div>
        ))}
      </div>

      <RenewalTimeline subs={all} />

      <div className="mv-grid-8-4 fade-in">
        {/* list */}
        <div>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
            <h3 style={{ fontSize: 15, fontWeight: 700 }}>{shown.length} assinaturas</h3>
            <div style={{ display: 'flex', gap: 3, padding: 3, background: 'var(--surface-2)', borderRadius: 9, border: '1px solid var(--line)' }}>
              {[[false,'Ativas'],[true,'Todas']].map(([k,l]) => (
                <button key={l} onClick={()=>setShowAll(k)} style={{ padding: '6px 13px', borderRadius: 7, fontSize: 12.5, fontWeight: 600,
                  color: showAll===k?'var(--frost-deep)':'var(--ink-4)', background: showAll===k?'var(--surface)':'transparent', boxShadow: showAll===k?'var(--sh-sm)':'none' }}>{l}</button>
              ))}
            </div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: 14 }}>
            {shown.map(s => <SubCard key={s.id} s={s} />)}
          </div>
        </div>

        {/* cost by category */}
        <div className="card" style={{ padding: '22px 24px', alignSelf: 'start' }}>
          <h3 style={{ fontSize: 14, fontWeight: 700, marginBottom: 18 }}>Custo por Categoria</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {catArr.map((c,i)=>(
              <div key={i}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 7 }}>
                  <span style={{ fontSize: 12.5, color: 'var(--ink-2)', fontWeight: 500 }}>{c.label}</span>
                  <span className="num" style={{ fontSize: 12.5, fontWeight: 700 }}>{MV.fmtBRL(c.value)}<span className="mono" style={{ fontSize: 10, color: 'var(--ink-4)', fontWeight: 400 }}>/mês</span></span>
                </div>
                <Progress value={c.value} max={monthlyTotal} height={7} threshold={false} color={c.color} />
              </div>
            ))}
          </div>
          <div style={{ marginTop: 20, paddingTop: 16, borderTop: '1px solid var(--line)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: 12.5, color: 'var(--ink-3)' }}>Total mensal</span>
            <span className="num" style={{ fontSize: 16, fontWeight: 700 }}>{MV.fmtBRL(monthlyTotal)}</span>
          </div>
        </div>
      </div>
    </div>
  );
}

window.Subscriptions = Subscriptions;
