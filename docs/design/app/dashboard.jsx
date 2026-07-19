/* Dashboard — the hero screen */

function AttentionStrip() {
  const cm = MV.data.catMap;
  // imminent obligations (due within 2 days)
  const urgent = [...MV.data.obligations]
    .filter(o => o.days <= 2)
    .sort((a, b) => a.days - b.days)
    .map(o => ({
      id: 'ob-' + o.id,
      icon: o.kind === 'fatura' ? 'card' : 'repeat',
      label: o.name,
      note: 'vence ' + MV.relDays(o.days),
      tone: o.days <= 1 ? 'neg' : 'warn',
      amount: o.amount,
    }));
  // categories over their monthly limit, worst overage first
  const over = MV.data.budgets
    .filter(b => b.spent >= b.limit)
    .map(b => ({ b, pct: (b.spent / b.limit) * 100 }))
    .sort((a, b) => b.pct - a.pct)
    .map(({ b, pct }) => {
      const cat = cm[b.categoryId];
      return {
        id: 'bg-' + b.id,
        icon: cat.icon,
        label: cat.name,
        note: pct.toFixed(0) + '% do orçamento',
        tone: 'neg',
        amount: b.spent - b.limit,
      };
    });

  // most urgent obligation + worst over-budget lead; cap at 3
  const chips = [...urgent, ...over].slice(0, 3);
  if (chips.length === 0) return null;

  const toneInk = { neg: 'var(--neg-ink)', warn: 'var(--warn)' };
  const toneBg  = { neg: 'var(--neg-bg)',  warn: 'var(--warn-bg)' };

  return (
    <div className="fade-in" style={{ display: 'flex', alignItems: 'center', gap: 14, flexWrap: 'wrap' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
        <span style={{ position: 'relative', width: 8, height: 8, flexShrink: 0 }}>
          <span style={{ position: 'absolute', inset: 0, borderRadius: '50%', background: 'var(--neg)' }} />
          <span style={{ position: 'absolute', inset: -3, borderRadius: '50%', border: '1px solid var(--neg)', opacity: 0.35 }} />
        </span>
        <span className="eyebrow" style={{ color: 'var(--ink-3)' }}>Precisa de atenção</span>
      </div>
      <div style={{ display: 'flex', gap: 9, flexWrap: 'wrap', flex: 1 }}>
        {chips.map(c => (
          <button key={c.id} className="card" style={{
            display: 'flex', alignItems: 'center', gap: 10, padding: '8px 13px 8px 9px',
            borderRadius: 11, background: 'var(--surface)', cursor: 'pointer', transition: 'border-color .15s',
          }}>
            <span style={{ width: 26, height: 26, borderRadius: 8, background: toneBg[c.tone], color: toneInk[c.tone], display: 'grid', placeItems: 'center', flexShrink: 0 }}>
              <Icon name={c.icon} size={14} />
            </span>
            <span style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.25, textAlign: 'left' }}>
              <span style={{ fontSize: 12.5, fontWeight: 600, color: 'var(--ink)', whiteSpace: 'nowrap' }}>{c.label}</span>
              <span className="mono" style={{ fontSize: 10.5, fontWeight: 600, color: toneInk[c.tone], letterSpacing: '.02em' }}>{c.note}</span>
            </span>
            <span className="num" style={{ fontSize: 12.5, fontWeight: 700, color: 'var(--ink-2)', marginLeft: 4 }}>{MV.fmtBRL(c.amount, {cents:false})}</span>
          </button>
        ))}
      </div>
    </div>
  );
}

function HeroBalance() {
  const { hero } = MV.data;
  const [hidden, setHidden] = React.useState(false);
  const p = MV.fmtBRLParts(hero.saldoLiquido);
  const kpis = [
    { label: 'Receitas',        value: hero.receitas,    pos: true,  spark: [4,6,5,8,7,9,11,10], color: 'var(--pos)' },
    { label: 'Despesas',        value: hero.despesas,    pos: false, spark: [8,7,9,6,8,7,6,5],   color: 'var(--neg)' },
    { label: 'Saldo do mês',    value: hero.saldoMes,    pos: true,  spark: [3,4,4,6,7,7,9,10],  color: 'var(--frost-deep)' },
    { label: 'Investido no mês',value: hero.investidoMes,pos: true,  spark: [2,3,3,4,5,5,6,7],   color: 'var(--frost-pine)', demote: true },
  ];
  return (
    <div className="card fade-in" style={{ padding: 0, overflow: 'hidden', display: 'grid', gridTemplateColumns: 'minmax(280px, 0.95fr) 1.25fr' }}>
      {/* left — saldo líquido */}
      <div style={{ padding: '26px 30px', borderRight: '1px solid var(--line)', position: 'relative',
        background: 'var(--hero-grad)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span className="eyebrow">Saldo Líquido Disponível</span>
          <button onClick={() => setHidden(h => !h)} style={{ color: 'var(--ink-4)', display: 'grid', placeItems: 'center' }} title="Ocultar saldo">
            <Icon name="eye" size={15} />
          </button>
        </div>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 3, marginTop: 14, color: 'var(--ink)' }}>
          <span style={{ fontSize: 20, fontWeight: 600, color: 'var(--ink-3)', alignSelf: 'flex-start', marginTop: 6 }}>{p.symbol}</span>
          <span className="num" style={{ fontFamily: 'var(--display)', fontSize: 48, fontWeight: 500, letterSpacing: '-.02em', lineHeight: 1 }}>
            {hidden ? '••••••' : p.int}
          </span>
          {!hidden && <span className="num" style={{ fontFamily: 'var(--display)', fontSize: 25, fontWeight: 500, color: 'var(--ink-3)' }}>,{p.dec}</span>}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 16 }}>
          <span className="pill" style={{ background: 'var(--pos-bg)', color: 'var(--pos-ink)' }}>
            <Icon name="arrowUp" size={12} stroke={2.6} /> {MV.fmtSignedPct(MV.data.hero.deltaSaldo)}
          </span>
          <span style={{ fontSize: 12.5, color: 'var(--ink-3)' }}>vs. mês anterior</span>
        </div>

        <div style={{ marginTop: 24, display: 'flex', flexDirection: 'column', gap: 9, paddingTop: 18, borderTop: '1px dashed var(--line-2)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: 12.5, color: 'var(--ink-3)', display: 'flex', alignItems: 'center', gap: 7 }}>
              <span style={{ width: 7, height: 7, borderRadius: 2, background: 'var(--frost-soft)' }} /> Em contas ativas</span>
            <span className="mono" style={{ fontSize: 13, fontWeight: 600, color: 'var(--ink-2)' }}>{MV.fmtBRL(MV.data.hero.contas)}</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: 12.5, color: 'var(--ink-3)', display: 'flex', alignItems: 'center', gap: 7 }}>
              <span style={{ width: 7, height: 7, borderRadius: 2, background: 'var(--neg)' }} /> Faturas em aberto</span>
            <span className="mono" style={{ fontSize: 13, fontWeight: 600, color: 'var(--neg-ink)' }}>− {MV.fmtBRL(MV.data.hero.openInvoices)}</span>
          </div>
        </div>
      </div>

      {/* right — KPI grid */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gridTemplateRows: '1fr 1fr' }}>
        {kpis.map((k, i) => (
          <div key={i} style={{
            padding: '22px 24px',
            borderRight: i % 2 === 0 ? '1px solid var(--line)' : 'none',
            borderBottom: i < 2 ? '1px solid var(--line)' : 'none',
            display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
            position: 'relative',
          }}>
            <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: 2.5, borderRadius: '0 0 3px 3px', background: k.color, opacity: k.demote ? 0.28 : 0.55 }} />
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <span style={{ fontSize: 12, color: 'var(--ink-3)', fontWeight: 500, paddingTop: 4 }}>{k.label}</span>
              <Sparkline data={k.spark} color={k.demote ? 'var(--ink-4)' : k.color} width={54} height={24} />
            </div>
            <div>
              <div className="num" style={{ fontSize: k.demote ? 17 : 22, fontWeight: 700, letterSpacing: '-.025em', color: k.demote ? 'var(--ink-2)' : 'var(--ink)', lineHeight: 1 }}>
                {k.label === 'Despesas' ? '− ' : ''}{MV.fmtBRL(k.value, {cents:false})}
              </div>
              <div className="mono" style={{ fontSize: 10, color: k.demote ? 'var(--ink-4)' : k.color, fontWeight: 600, marginTop: 5, letterSpacing: '.04em' }}>
                {k.label === 'Receitas' ? 'ENTRADAS' : k.label === 'Despesas' ? 'SAÍDAS' : k.label === 'Investido no mês' ? 'INVESTIDO' : 'RESULTADO'}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function CashFlowCard() {
  const [period, setPeriod] = React.useState('30d');
  const series = { '7d': MV.data.cashflow7, '30d': MV.data.cashflow30, '12m': MV.data.cashflow12m };
  const data = series[period];
  const labels = period === '12m'
    ? MV.MESES.map(m => m.toUpperCase())
    : data.map((_, i) => period === '7d' ? ['Seg','Ter','Qua','Qui','Sex','Sáb','Dom'][i] : `${i+1}`);
  const first = data[0], last = data[data.length-1];
  const delta = ((last - first) / first) * 100;
  return (
    <div className="card fade-in" style={{ padding: '22px 24px' }}>
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 6 }}>
        <div>
          <h3 style={{ fontSize: 15, fontWeight: 700, letterSpacing: '-.01em' }}>Fluxo de Caixa</h3>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 10, marginTop: 6 }}>
            <span className="num" style={{ fontSize: 24, fontWeight: 700, letterSpacing: '-.02em' }}>{MV.fmtBRL(last)}</span>
            <span className="pill" style={{ background: delta>=0?'var(--pos-bg)':'var(--neg-bg)', color: delta>=0?'var(--pos-ink)':'var(--neg-ink)' }}>
              <Icon name={delta>=0?'arrowUpRight':'arrowDownRight'} size={11} stroke={2.5} /> {MV.fmtSignedPct(delta)}
            </span>
          </div>
        </div>
        <div style={{ display: 'flex', gap: 3, padding: 3, background: 'var(--surface-2)', borderRadius: 9, border: '1px solid var(--line)' }}>
          {['7d','30d','12m'].map(p => (
            <button key={p} onClick={() => setPeriod(p)} className="mono" style={{
              padding: '5px 11px', borderRadius: 7, fontSize: 11.5, fontWeight: 600,
              color: period===p ? 'var(--frost-deep)' : 'var(--ink-4)',
              background: period===p ? 'var(--surface)' : 'transparent',
              boxShadow: period===p ? 'var(--sh-sm)' : 'none',
            }}>{p === '7d' ? '7 dias' : p === '30d' ? '30 dias' : '12 meses'}</button>
          ))}
        </div>
      </div>
      <Spline data={data} labels={labels} height={230} fmt={(v)=>MV.fmtBRL(v)} id="cashflow" />
    </div>
  );
}

function ExpenseDonut() {
  const data = MV.data.expenseDist;
  const [active, setActive] = React.useState(null);
  const total = data.reduce((s,d)=>s+d.value,0);
  const shown = active ? data.find(d=>d.id===active) : null;
  return (
    <div className="card fade-in" style={{ padding: '22px 24px', display: 'flex', flexDirection: 'column' }}>
      <h3 style={{ fontSize: 15, fontWeight: 700, letterSpacing: '-.01em' }}>Distribuição de Gastos</h3>
      <div style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 2 }}>Junho · por categoria</div>
      <div style={{ position: 'relative', alignSelf: 'center', margin: '14px 0 18px' }}>
        <Donut data={data} size={184} thickness={24} active={active} onSlice={setActive} />
        <div style={{ position: 'absolute', inset: 0, display: 'grid', placeItems: 'center', textAlign: 'center', pointerEvents: 'none' }}>
          <div>
            <div className="eyebrow" style={{ fontSize: 9.5 }}>{shown ? shown.label : 'Total'}</div>
            <div className="num" style={{ fontSize: 20, fontWeight: 700, letterSpacing: '-.02em', marginTop: 3 }}>{MV.fmtBRL(shown ? shown.value : total, {cents:false})}</div>
            {shown && <div className="mono" style={{ fontSize: 11, color: 'var(--ink-4)', marginTop: 2 }}>{((shown.value/total)*100).toFixed(0)}%</div>}
          </div>
        </div>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
        {data.map(d => (
          <button key={d.id} onMouseEnter={()=>setActive(d.id)} onMouseLeave={()=>setActive(null)}
            style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '6px 8px', borderRadius: 8,
              background: active===d.id ? 'var(--surface-2)' : 'transparent', transition: 'background .15s' }}>
            <span style={{ width: 9, height: 9, borderRadius: 3, background: d.color, flexShrink: 0 }} />
            <span style={{ fontSize: 13, color: 'var(--ink-2)', flex: 1, textAlign: 'left' }}>{d.label}</span>
            <span className="mono" style={{ fontSize: 12, fontWeight: 600, color: 'var(--ink)' }}>{MV.fmtBRL(d.value, {cents:false})}</span>
            <span className="mono" style={{ fontSize: 11, color: 'var(--ink-4)', width: 34, textAlign: 'right' }}>{((d.value/total)*100).toFixed(0)}%</span>
          </button>
        ))}
      </div>
    </div>
  );
}

function ObligationsRadar() {
  const items = [...MV.data.obligations].sort((a,b)=>a.days-b.days);
  const urgColor = (d) => d <= 2 ? 'var(--neg)' : d <= 5 ? 'var(--warn)' : 'var(--pos)';
  const urgBg = (d) => d <= 2 ? 'var(--neg-bg)' : d <= 5 ? 'var(--warn-bg)' : 'var(--pos-bg)';
  return (
    <div className="card fade-in" style={{ padding: '22px 24px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
        <div>
          <h3 style={{ fontSize: 15, fontWeight: 700, letterSpacing: '-.01em' }}>Radar de Obrigações</h3>
          <div style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 2 }}>Próximos 7 dias</div>
        </div>
        <span className="pill" style={{ background: 'var(--surface-3)', color: 'var(--ink-3)' }}>{items.length} itens</span>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 7 }}>
        {items.map(o => (
          <div key={o.id} style={{ display: 'flex', alignItems: 'center', gap: 13, padding: '11px 13px', borderRadius: 12, background: 'var(--surface-2)', border: '1px solid var(--line)' }}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: urgBg(o.days), color: urgColor(o.days), display: 'grid', placeItems: 'center', flexShrink: 0 }}>
              <Icon name={o.kind === 'fatura' ? 'card' : 'repeat'} size={18} />
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 13.5, fontWeight: 600, color: 'var(--ink)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{o.name}</div>
              <div style={{ fontSize: 11.5, color: 'var(--ink-3)', textTransform: 'capitalize' }}>{o.kind} · vence {MV.relDays(o.days)}</div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div className="num" style={{ fontSize: 14, fontWeight: 700 }}>{MV.fmtBRL(o.amount)}</div>
              <div className="mono" style={{ fontSize: 10, fontWeight: 600, color: urgColor(o.days), marginTop: 1 }}>{MV.relDays(o.days).toUpperCase()}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function BudgetsMini() {
  const cm = MV.data.catMap;
  return (
    <div className="card fade-in" style={{ padding: '22px 24px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
        <div>
          <h3 style={{ fontSize: 15, fontWeight: 700, letterSpacing: '-.01em' }}>Orçamentos do Mês</h3>
          <div style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 2 }}>{MV.data.budgets.length} categorias acompanhadas</div>
        </div>
        <button className="mono" style={{ fontSize: 11.5, fontWeight: 600, color: 'var(--frost-deep)', display: 'flex', alignItems: 'center', gap: 4 }}>
          Ver tudo <Icon name="chevRight" size={13} />
        </button>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px 26px' }}>
        {MV.data.budgets.map(b => {
          const cat = cm[b.categoryId];
          const pct = (b.spent / b.limit) * 100;
          const over = pct >= 100;
          return (
            <div key={b.id}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 9, marginBottom: 9 }}>
                <span style={{ width: 26, height: 26, borderRadius: 8, background: cat.color+'1f', color: cat.color, display: 'grid', placeItems: 'center', flexShrink: 0 }}>
                  <Icon name={cat.icon} size={14} />
                </span>
                <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--ink)', flex: 1 }}>{cat.name}</span>
                <span className="mono" style={{ fontSize: 11, fontWeight: 600, color: over?'var(--neg-ink)':pct>=80?'var(--warn)':'var(--ink-3)' }}>{pct.toFixed(0)}%</span>
              </div>
              <Progress value={b.spent} max={b.limit} height={7} />
              <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 7 }}>
                <span className="mono" style={{ fontSize: 11, color: 'var(--ink-2)', fontWeight: 600 }}>{MV.fmtBRL(b.spent, {cents:false})}</span>
                <span className="mono" style={{ fontSize: 11, color: 'var(--ink-4)' }}>de {MV.fmtBRL(b.limit, {cents:false})}</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function Dashboard() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <AttentionStrip />
      <HeroBalance />
      <div className="mv-grid-8-4">
        <CashFlowCard />
        <ExpenseDonut />
      </div>
      <div className="mv-grid-5-7">
        <ObligationsRadar />
        <BudgetsMini />
      </div>
    </div>
  );
}

window.Dashboard = Dashboard;
