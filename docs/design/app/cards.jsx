/* Card Management — wallet + invoice cycle monitor */

function CreditCardVisual({ c, selected, onClick }) {
  const used = c.used, avail = c.limit - c.used;
  const pct = (used / c.limit) * 100;
  return (
    <button onClick={onClick} style={{
      textAlign: 'left', width: '100%', borderRadius: 18, padding: '20px 22px',
      background: `linear-gradient(135deg, ${c.color} 0%, ${shade(c.color,-18)} 100%)`,
      color: '#fff', position: 'relative', overflow: 'hidden', aspectRatio: '1.6 / 1',
      boxShadow: selected ? '0 0 0 2px var(--frost-deep), var(--sh-lg)' : 'var(--sh-md)',
      transform: selected ? 'translateY(-2px)' : 'none', transition: 'all .2s',
      display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
    }}>
      <div style={{ position: 'absolute', top: -40, right: -30, width: 160, height: 160, borderRadius: 99, background: 'rgba(255,255,255,.07)' }} />
      <div style={{ position: 'absolute', bottom: -60, right: 20, width: 140, height: 140, borderRadius: 99, background: 'rgba(255,255,255,.05)' }} />
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', position: 'relative', gap: 12 }}>
        <div style={{ minWidth: 0 }}>
          <div style={{ fontSize: 14, fontWeight: 700, letterSpacing: '-.01em', lineHeight: 1.2 }}>{c.name}</div>
          <div className="mono" style={{ fontSize: 10, opacity: .7, marginTop: 4, letterSpacing: '.08em', textTransform: 'uppercase' }}>{c.institution} · {c.cardType === 'VIRTUAL' ? 'Virtual' : 'Físico'}</div>
        </div>
        <div style={{ width: 34, height: 24, borderRadius: 5, flexShrink: 0, background: 'linear-gradient(135deg, rgba(255,255,255,.85), rgba(255,255,255,.55))', opacity: .9 }} />
      </div>
      <div style={{ position: 'relative' }}>
        <div className="mono" style={{ fontSize: 16, letterSpacing: '.12em', marginBottom: 12 }}>•••• {c.last4}</div>
        <div style={{ height: 5, background: 'rgba(255,255,255,.22)', borderRadius: 99, overflow: 'hidden' }}>
          <div style={{ width: `${pct}%`, height: '100%', background: pct>=80?'#EBCB8B':'rgba(255,255,255,.92)', borderRadius: 99 }} />
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 8 }}>
          <div>
            <div style={{ fontSize: 9.5, opacity: .6, textTransform: 'uppercase', letterSpacing: '.08em' }}>Disponível</div>
            <div className="num" style={{ fontSize: 14, fontWeight: 700 }}>{MV.fmtBRL(avail, {cents:false})}</div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: 9.5, opacity: .6, textTransform: 'uppercase', letterSpacing: '.08em' }}>Limite</div>
            <div className="num" style={{ fontSize: 14, fontWeight: 700, opacity: .85 }}>{MV.fmtBRL(c.limit, {cents:false})}</div>
          </div>
        </div>
      </div>
    </button>
  );
}

function shade(hex, pct) {
  const n = parseInt(hex.slice(1), 16);
  let r = (n >> 16) + Math.round(255 * pct / 100);
  let g = ((n >> 8) & 255) + Math.round(255 * pct / 100);
  let b = (n & 255) + Math.round(255 * pct / 100);
  r = Math.max(0, Math.min(255, r)); g = Math.max(0, Math.min(255, g)); b = Math.max(0, Math.min(255, b));
  return `rgb(${r},${g},${b})`;
}

const INV_STATUS = {
  OPEN: { label: 'Aberta', bg: 'var(--accent-bg)', col: 'var(--frost-deep)' },
  CLOSED: { label: 'Fechada', bg: 'var(--warn-bg)', col: 'var(--warn)' },
  PAID: { label: 'Paga', bg: 'var(--pos-bg)', col: 'var(--pos-ink)' },
};

// sample fatura transactions
const FATURA_TX = [
  { id:'f1', date:'2026-06-06', desc:'iFood • Almoço', cat:'alim-deliv', amount:6480 },
  { id:'f2', date:'2026-06-04', desc:'Netflix', cat:'serv-stream', amount:5590 },
  { id:'f3', date:'2026-06-03', desc:'Uber • Centro', cat:'trans-app', amount:3120 },
  { id:'f4', date:'2026-06-01', desc:'Restaurante Fasano', cat:'alim-rest', amount:18600 },
  { id:'f5', date:'2026-05-31', desc:'Spotify Família', cat:'serv-stream', amount:3490 },
  { id:'f6', date:'2026-05-28', desc:'Cinema Cinemark', cat:'lazer', amount:4800 },
  { id:'f7', date:'2026-05-26', desc:'Amazon • Eletrônicos', cat:'outros', amount:44520 },
];

function Cards() {
  const [sel, setSel] = React.useState('cc1');
  const card = MV.data.cards.find(c => c.id === sel);
  const invoices = MV.data.invoices.filter(i => i.cardId === sel);
  const [invSel, setInvSel] = React.useState('inv-c');
  React.useEffect(() => {
    const open = MV.data.invoices.find(i => i.cardId === sel && i.status === 'OPEN');
    setInvSel(open ? open.id : (MV.data.invoices.find(i=>i.cardId===sel)||{}).id);
  }, [sel]);
  const inv = invoices.find(i => i.id === invSel) || invoices[invoices.length-1];
  const bestDay = card.closingDay + 1;
  const cm = MV.data.catMap;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <div className="fade-in">
        <h2 style={{ fontSize: 15, fontWeight: 700, marginBottom: 14 }}>Carteira</h2>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 16 }}>
          {MV.data.cards.map(c => <CreditCardVisual key={c.id} c={c} selected={sel===c.id} onClick={()=>setSel(c.id)} />)}
        </div>
      </div>

      <div className="mv-cards-grid fade-in">
        {/* invoice monitor */}
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{ padding: '18px 22px', borderBottom: '1px solid var(--line)' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <h3 style={{ fontSize: 15, fontWeight: 700 }}>Monitor de Faturas · {card.name}</h3>
            </div>
            <div style={{ display: 'flex', gap: 8, marginTop: 16, overflowX: 'auto' }}>
              {invoices.map(i => {
                const st = INV_STATUS[i.status];
                const on = i.id === invSel;
                return (
                  <button key={i.id} onClick={()=>setInvSel(i.id)} style={{
                    flex: '0 0 auto', padding: '10px 16px', borderRadius: 12, textAlign: 'left',
                    border: '1px solid '+(on?'var(--accent-line)':'var(--line)'),
                    background: on?'var(--accent-bg)':'var(--surface)', minWidth: 110,
                  }}>
                    <div className="mono" style={{ fontSize: 10, color: 'var(--ink-4)', textTransform: 'uppercase', letterSpacing: '.06em' }}>
                      {MV.MESES_FULL[parseInt(i.ref.split('-')[1])-1].slice(0,3)} {i.ref.split('-')[0]}
                    </div>
                    <div className="num" style={{ fontSize: 15, fontWeight: 700, margin: '4px 0', color: on?'var(--frost-deep)':'var(--ink)' }}>{MV.fmtBRL(i.total, {cents:false})}</div>
                    <span className="pill" style={{ fontSize: 9.5, padding: '2px 7px', background: st.bg, color: st.col }}>{st.label}</span>
                  </button>
                );
              })}
            </div>
          </div>
          {/* selected invoice detail */}
          <div style={{ padding: '16px 22px', display: 'flex', gap: 24, flexWrap: 'wrap', borderBottom: '1px solid var(--line)', background: 'var(--surface-2)' }}>
            <div><div className="eyebrow" style={{ fontSize: 9 }}>Fechamento</div><div style={{ fontSize: 13.5, fontWeight: 600, marginTop: 3 }}>{MV.fmtDate(inv.closing)}</div></div>
            <div><div className="eyebrow" style={{ fontSize: 9 }}>Vencimento</div><div style={{ fontSize: 13.5, fontWeight: 600, marginTop: 3 }}>{MV.fmtDate(inv.due)}</div></div>
            <div><div className="eyebrow" style={{ fontSize: 9 }}>Total</div><div className="num" style={{ fontSize: 13.5, fontWeight: 700, marginTop: 3 }}>{MV.fmtBRL(inv.total)}</div></div>
            <div style={{ marginLeft: 'auto', alignSelf: 'center' }}>
              {inv.status === 'CLOSED' && <button style={{ padding: '9px 16px', background: 'var(--frost-deep)', color: '#fff', borderRadius: 10, fontWeight: 600, fontSize: 13 }}>Registrar Pagamento</button>}
              {inv.status === 'OPEN' && <span className="pill" style={{ background: 'var(--accent-bg)', color: 'var(--frost-deep)' }}>Em andamento</span>}
              {inv.status === 'PAID' && <span className="pill" style={{ background: 'var(--pos-bg)', color: 'var(--pos-ink)' }}><Icon name="check" size={12} /> Paga</span>}
            </div>
          </div>
          <div>
            {FATURA_TX.map(t => {
              const cat = cm[t.cat]; const d = MV.fmtDayMonth(t.date);
              return (
                <div key={t.id} style={{ display: 'flex', alignItems: 'center', gap: 13, padding: '12px 22px', borderBottom: '1px solid var(--line)' }}>
                  <div style={{ textAlign: 'center', width: 34 }}>
                    <div className="num" style={{ fontSize: 14, fontWeight: 700, lineHeight: 1 }}>{d.day}</div>
                    <div className="mono" style={{ fontSize: 9, color: 'var(--ink-4)', textTransform: 'uppercase' }}>{d.mon}</div>
                  </div>
                  <span style={{ width: 32, height: 32, borderRadius: 9, background: cat.color+'1c', color: cat.color, display: 'grid', placeItems: 'center' }}><Icon name={cat.icon} size={15} /></span>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13, fontWeight: 600 }}>{t.desc}</div>
                    <div style={{ fontSize: 11.5, color: 'var(--ink-4)' }}>{cat.name}</div>
                  </div>
                  <div className="num" style={{ fontSize: 13.5, fontWeight: 700 }}>{MV.fmtBRL(t.amount)}</div>
                </div>
              );
            })}
          </div>
        </div>

        {/* side panel — limits + best day */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div className="card" style={{ padding: '22px 24px' }}>
            <h3 style={{ fontSize: 14, fontWeight: 700, marginBottom: 16 }}>Limite de Crédito</h3>
            <div style={{ display: 'grid', placeItems: 'center', marginBottom: 16 }}>
              <Ring value={card.used} max={card.limit} size={130} thickness={11} color={card.used/card.limit>=0.8?'var(--warn)':'var(--frost-deep)'}>
                <div style={{ textAlign: 'center' }}>
                  <div className="num" style={{ fontSize: 22, fontWeight: 700 }}>{((card.used/card.limit)*100).toFixed(0)}%</div>
                  <div style={{ fontSize: 10.5, color: 'var(--ink-4)' }}>utilizado</div>
                </div>
              </Ring>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 11 }}>
              {[['Limite total', card.limit, 'var(--ink)'],['Utilizado', card.used, 'var(--neg-ink)'],['Disponível', card.limit-card.used, 'var(--pos-ink)']].map(([l,v,c],i)=>(
                <div key={i} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: i?11:0, borderTop: i?'1px solid var(--line)':'none', gap: 8 }}>
                  <span style={{ fontSize: 12.5, color: 'var(--ink-3)', whiteSpace: 'nowrap' }}>{l}</span>
                  <span className="num" style={{ fontSize: 13.5, fontWeight: 700, color: c, whiteSpace: 'nowrap' }}>{MV.fmtBRL(v, {cents:false})}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="card" style={{ padding: '20px 24px', background: 'var(--accent-bg)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 9, marginBottom: 12 }}>
              <span style={{ width: 32, height: 32, borderRadius: 9, background: 'var(--frost-pine)', color: '#fff', display: 'grid', placeItems: 'center' }}><Icon name="flame" size={16} fill /></span>
              <h3 style={{ fontSize: 14, fontWeight: 700 }}>Melhor Dia de Compra</h3>
            </div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
              <span className="num" style={{ fontSize: 38, fontWeight: 800, letterSpacing: '-.03em', color: 'var(--frost-deep)' }}>{bestDay}</span>
              <span style={{ fontSize: 13, color: 'var(--ink-3)' }}>de cada mês</span>
            </div>
            <p style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 10, lineHeight: 1.5 }}>
              Compras neste dia entram na próxima fatura, maximizando o prazo sem juros. Fechamento dia {card.closingDay}.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

window.Cards = Cards;
