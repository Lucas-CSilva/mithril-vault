/* Planning — Budgets + Goals (Cofres) */

function BudgetCard({ b }) {
  const cat = MV.data.catMap[b.categoryId];
  const pct = (b.spent / b.limit) * 100;
  const over = pct >= 100;
  const remaining = b.limit - b.spent;
  const daysLeft = 24; // days remaining in month (mock)
  return (
    <div className="card" style={{ padding: '18px 20px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 11, marginBottom: 14 }}>
        <span style={{ width: 38, height: 38, borderRadius: 11, background: cat.color+'1f', color: cat.color, display: 'grid', placeItems: 'center', flexShrink: 0 }}>
          <Icon name={cat.icon} size={18} />
        </span>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 14, fontWeight: 600 }}>{cat.name}</div>
          <div style={{ fontSize: 11.5, color: 'var(--ink-4)' }}>{daysLeft} dias restantes</div>
        </div>
        <span className="pill" style={{ background: over?'var(--neg-bg)':pct>=80?'var(--warn-bg)':'var(--pos-bg)', color: over?'var(--neg-ink)':pct>=80?'var(--warn)':'var(--pos-ink)' }}>{pct.toFixed(0)}%</span>
      </div>
      <Progress value={b.spent} max={b.limit} height={9} />
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginTop: 12 }}>
        <div>
          <span className="num" style={{ fontSize: 16, fontWeight: 700, color: over?'var(--neg-ink)':'var(--ink)' }}>{MV.fmtBRL(b.spent)}</span>
          <span className="mono" style={{ fontSize: 11.5, color: 'var(--ink-4)' }}> / {MV.fmtBRL(b.limit, {cents:false})}</span>
        </div>
        <span className="mono" style={{ fontSize: 11.5, fontWeight: 600, color: over?'var(--neg-ink)':'var(--ink-3)' }}>
          {over ? `+${MV.fmtBRL(Math.abs(remaining),{cents:false})}` : `restam ${MV.fmtBRL(remaining,{cents:false})}`}
        </span>
      </div>
    </div>
  );
}

function GoalCard({ g }) {
  const pct = (g.current / g.target) * 100;
  const done = pct >= 100;
  const remaining = g.target - g.current;
  let projection;
  if (g.deadline) {
    const months = Math.max(1, Math.round((new Date(g.deadline) - new Date('2026-06-07')) / (1000*60*60*24*30)));
    projection = `${MV.fmtBRL(Math.round(remaining/months), {cents:false})}/mês · meta ${MV.fmtDate(g.deadline)}`;
  } else {
    projection = 'Sem prazo definido';
  }
  return (
    <div className="card" style={{ padding: '22px 24px', position: 'relative', overflow: 'hidden' }}>
      {done && <div style={{ position: 'absolute', top: 14, right: 16 }}><span className="pill" style={{ background: 'var(--pos-bg)', color: 'var(--pos-ink)' }}><Icon name="check" size={12} /> Concluído</span></div>}
      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <Ring value={g.current} max={g.target} size={92} thickness={9} color={done?'var(--pos)':g.color}>
          <span style={{ width: 42, height: 42, borderRadius: 12, background: g.color+'1f', color: g.color, display: 'grid', placeItems: 'center' }}>
            <Icon name={g.icon} size={20} />
          </span>
        </Ring>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 15, fontWeight: 700, letterSpacing: '-.01em' }}>{g.name}</div>
          <div className="num" style={{ fontSize: 13, color: 'var(--ink-3)', marginTop: 4 }}>
            <span style={{ fontWeight: 700, color: 'var(--ink)', fontSize: 18 }}>{MV.fmtBRL(g.current, {cents:false})}</span>
            <span className="mono" style={{ fontSize: 12, color: 'var(--ink-4)' }}> / {MV.fmtBRL(g.target, {cents:false})}</span>
          </div>
          <div className="mono" style={{ fontSize: 12, fontWeight: 700, color: done?'var(--pos-ink)':g.color, marginTop: 3 }}>{pct.toFixed(0)}%</div>
        </div>
      </div>
      <div style={{ marginTop: 16, paddingTop: 14, borderTop: '1px solid var(--line)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span style={{ fontSize: 11.5, color: 'var(--ink-3)', display: 'flex', alignItems: 'center', gap: 6 }}>
          <Icon name="clock" size={13} style={{ color: 'var(--ink-4)' }} /> {projection}
        </span>
        <button className="mono" style={{ fontSize: 11, fontWeight: 600, color: 'var(--frost-deep)', padding: '5px 11px', background: 'var(--accent-bg)', borderRadius: 8 }}>Aportar</button>
      </div>
    </div>
  );
}

function PlanningSummary({ totalBudget, totalSpent, totalGoals, totalTargets }) {
  const pct = (totalSpent / totalBudget) * 100;
  const daysLeft = 24;
  const perDay = totalSpent / (30 - daysLeft);
  const projected = Math.round(perDay * 30);
  const projOver = projected > totalBudget;
  return (
    <div className="card fade-in" style={{ padding: '18px 26px', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '18px 32px', alignItems: 'center' }}>
      <div>
        <div className="eyebrow" style={{ fontSize: 9.5, marginBottom: 8 }}>Saúde do orçamento · Junho</div>
        <div style={{ marginBottom: 8 }}><Progress value={totalSpent} max={totalBudget} height={9} /></div>
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span className="num" style={{ fontSize: 13, fontWeight: 700 }}>{MV.fmtBRL(totalSpent, {cents:false})} gastos</span>
          <span className="mono" style={{ fontSize: 11, color: 'var(--ink-4)' }}>de {MV.fmtBRL(totalBudget, {cents:false})}</span>
        </div>
      </div>
      <div style={{ borderLeft: '1px solid var(--line)', paddingLeft: 24 }}>
        <div className="eyebrow" style={{ fontSize: 9.5, marginBottom: 6 }}>Projeção do mês</div>
        <div className="num" style={{ fontSize: 20, fontWeight: 700, color: projOver ? 'var(--neg-ink)' : 'var(--ink)' }}>{MV.fmtBRL(projected, {cents:false})}</div>
        <div style={{ fontSize: 12, color: projOver ? 'var(--neg-ink)' : 'var(--ink-3)', marginTop: 3 }}>
          {projOver ? `Ríscio de extrapolar em ${MV.fmtBRL(projected-totalBudget, {cents:false})}` : `${MV.fmtBRL(totalBudget-projected, {cents:false})} de folga projetada`}
        </div>
      </div>
      <div style={{ borderLeft: '1px solid var(--line)', paddingLeft: 24 }}>
        <div className="eyebrow" style={{ fontSize: 9.5, marginBottom: 6 }}>Cofres</div>
        <div className="num" style={{ fontSize: 20, fontWeight: 700 }}>{MV.fmtBRL(totalGoals, {cents:false})}</div>
        <div style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 3 }}>guardados de {MV.fmtBRL(totalTargets, {cents:false})}</div>
      </div>
      <div style={{ borderLeft: '1px solid var(--line)', paddingLeft: 24 }}>
        <div className="eyebrow" style={{ fontSize: 9.5, marginBottom: 6 }}>Dias restantes</div>
        <div className="num" style={{ fontSize: 20, fontWeight: 700 }}>{daysLeft}</div>
        <div style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 3 }}>de Junho restantes</div>
      </div>
    </div>
  );
}

function Planning() {
  const totalBudget = MV.data.budgets.reduce((s,b)=>s+b.limit,0);
  const totalSpent = MV.data.budgets.reduce((s,b)=>s+b.spent,0);
  const totalGoals = MV.data.goals.reduce((s,g)=>s+g.current,0);
  const totalTargets = MV.data.goals.reduce((s,g)=>s+g.target,0);
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
      <PlanningSummary totalBudget={totalBudget} totalSpent={totalSpent} totalGoals={totalGoals} totalTargets={totalTargets} />
      {/* Budgets */}
      <section className="fade-in">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 14 }}>
            <h2 style={{ fontSize: 17, fontWeight: 700, letterSpacing: '-.01em' }}>Orçamentos</h2>
            <span style={{ fontSize: 13, color: 'var(--ink-3)' }}>
              <span className="num" style={{ fontWeight: 700, color: 'var(--ink)' }}>{MV.fmtBRL(totalSpent, {cents:false})}</span> de <span className="num">{MV.fmtBRL(totalBudget, {cents:false})}</span> · Junho
            </span>
          </div>
          <button className="mono" style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, fontWeight: 600, color: 'var(--frost-deep)', padding: '7px 12px', border: '1px solid var(--accent-line)', borderRadius: 9, background: 'var(--accent-bg)' }}>
            <Icon name="plus" size={14} stroke={2.4} /> Novo orçamento
          </button>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 14 }}>
          {MV.data.budgets.map(b => <BudgetCard key={b.id} b={b} />)}
        </div>
      </section>

      {/* Goals */}
      <section className="fade-in">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 14 }}>
            <h2 style={{ fontSize: 17, fontWeight: 700, letterSpacing: '-.01em' }}>Cofres</h2>
            <span style={{ fontSize: 13, color: 'var(--ink-3)' }}>
              <span className="num" style={{ fontWeight: 700, color: 'var(--ink)' }}>{MV.fmtBRL(totalGoals, {cents:false})}</span> guardados · {MV.data.goals.length} objetivos
            </span>
          </div>
          <button className="mono" style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, fontWeight: 600, color: 'var(--frost-deep)', padding: '7px 12px', border: '1px solid var(--accent-line)', borderRadius: 9, background: 'var(--accent-bg)' }}>
            <Icon name="plus" size={14} stroke={2.4} /> Novo cofre
          </button>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(330px, 1fr))', gap: 14 }}>
          {MV.data.goals.map(g => <GoalCard key={g.id} g={g} />)}
        </div>
      </section>
    </div>
  );
}

window.Planning = Planning;
