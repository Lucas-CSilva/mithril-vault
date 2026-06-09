/* Investments — renda fixa portfolio */

const INV_TYPE_LABEL = {
  CDB:'CDB', TESOURO_SELIC:'Tesouro Selic', TESOURO_IPCA:'Tesouro IPCA+', TESOURO_PREFIXADO:'Tesouro Pré',
  LCI:'LCI', LCA:'LCA', CRI:'CRI', CRA:'CRA', DEBENTURE:'Debênture',
};
const INV_TYPE_COLOR = {
  CDB:'#5E81AC', TESOURO_SELIC:'#88C0D0', TESOURO_IPCA:'#8FBCBB', LCI:'#7E9F69', CDB2:'#81A1C1',
};

function irRate(days) {
  if (days <= 180) return 2250; if (days <= 360) return 2000; if (days <= 720) return 1750; return 1500;
}
function computeInv(inv) {
  const grossYield = inv.gross - inv.invested;
  const days = Math.round((new Date('2026-06-07') - new Date(inv.start)) / (1000*60*60*24));
  const rate = inv.exempt ? 0 : irRate(days);
  const ir = Math.round((grossYield * rate) / 10000);
  const net = inv.gross - ir;
  return { grossYield, days, ir, net, grossPct: (grossYield/inv.invested)*100, netPct: ((net-inv.invested)/inv.invested)*100 };
}
function fmtRate(inv) {
  const v = inv.rate / 100;
  if (inv.rateType === 'IPCA_PLUS') return `IPCA + ${v.toLocaleString('pt-BR',{minimumFractionDigits:2})}%`;
  if (inv.rateType === 'CDI_PERCENTAGE') return `${v.toLocaleString('pt-BR')}% CDI`;
  if (inv.rateType === 'SELIC_PERCENTAGE') return `${v.toLocaleString('pt-BR')}% Selic`;
  return `${v.toLocaleString('pt-BR',{minimumFractionDigits:2})}%`;
}

function Investments() {
  const invs = MV.data.investments;
  const computed = invs.map(i => ({ ...i, c: computeInv(i) }));
  const totInvested = invs.reduce((s,i)=>s+i.invested,0);
  const totGross = invs.reduce((s,i)=>s+i.gross,0);
  const totIR = computed.reduce((s,i)=>s+i.c.ir,0);
  const totNet = totGross - totIR;
  const grossYieldPct = ((totGross-totInvested)/totInvested)*100;
  const netYieldPct = ((totNet-totInvested)/totInvested)*100;

  // allocation by type
  const byType = {};
  invs.forEach(i => { byType[i.type] = (byType[i.type]||0) + i.gross; });
  const alloc = Object.entries(byType).map(([t,v]) => ({ label: INV_TYPE_LABEL[t], value: v, color: INV_TYPE_COLOR[t] || '#99A1B0' }));

  const netWorth = [4800000,4920000,5010000,5180000,5240000,5360000,5480000,5610000,5720000,5810000,5890000,totGross];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      {/* hero overview */}
      <div className="card fade-in" style={{ padding: 0, display: 'grid', gridTemplateColumns: '1.1fr 1.3fr' }}>
        <div style={{ padding: '24px 28px', borderRight: '1px solid var(--line)', background: 'linear-gradient(160deg, #fbfcfe, #f4f7fb)' }}>
          <span className="eyebrow">Patrimônio Investido (bruto)</span>
          <div className="num" style={{ fontSize: 40, fontWeight: 700, letterSpacing: '-.03em', marginTop: 10 }}>{MV.fmtBRL(totGross)}</div>
          <div style={{ display: 'flex', gap: 10, marginTop: 14 }}>
            <span className="pill" style={{ background: 'var(--pos-bg)', color: 'var(--pos-ink)' }}><Icon name="arrowUpRight" size={12} stroke={2.5} /> {MV.fmtSignedPct(grossYieldPct)} bruto</span>
            <span className="pill" style={{ background: 'var(--surface-3)', color: 'var(--ink-3)' }}>{MV.fmtSignedPct(netYieldPct)} líquido</span>
          </div>
          <div style={{ marginTop: 22 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11.5, marginBottom: 8 }}>
              <span className="eyebrow" style={{ fontSize: 9 }}>Alocação por tipo</span>
            </div>
            <StackBar data={alloc} height={12} />
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px 16px', marginTop: 12 }}>
              {alloc.map((a,i)=>(
                <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span style={{ width: 8, height: 8, borderRadius: 2, background: a.color }} />
                  <span style={{ fontSize: 11.5, color: 'var(--ink-2)' }}>{a.label}</span>
                  <span className="mono" style={{ fontSize: 11, color: 'var(--ink-4)' }}>{((a.value/totGross)*100).toFixed(0)}%</span>
                </div>
              ))}
            </div>
          </div>
        </div>
        <div style={{ padding: '20px 24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
            <h3 style={{ fontSize: 14, fontWeight: 700 }}>Evolução do Patrimônio</h3>
            <span className="mono" style={{ fontSize: 10.5, color: 'var(--ink-4)' }}>12 MESES</span>
          </div>
          <Spline data={netWorth} labels={MV.MESES.map(m=>m.toUpperCase())} height={150} color="var(--frost-pine)" fmt={(v)=>MV.fmtBRL(v)} id="networth" />
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12, marginTop: 14, paddingTop: 16, borderTop: '1px solid var(--line)' }}>
            {[['Investido', totInvested, 'var(--ink-2)'],['IR estimado', totIR, 'var(--neg-ink)'],['Valor líquido', totNet, 'var(--pos-ink)']].map(([l,v,c],i)=>(
              <div key={i}>
                <div className="eyebrow" style={{ fontSize: 9 }}>{l}</div>
                <div className="num" style={{ fontSize: 15, fontWeight: 700, color: c, marginTop: 4 }}>{MV.fmtBRL(v, {cents:false})}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* positions */}
      <div className="card fade-in" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '18px 22px', borderBottom: '1px solid var(--line)' }}>
          <h3 style={{ fontSize: 15, fontWeight: 700 }}>Posições · {invs.length} ativos</h3>
          <button className="mono" style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, fontWeight: 600, color: 'var(--frost-deep)', padding: '7px 12px', border: '1px solid var(--accent-line)', borderRadius: 9, background: 'var(--accent-bg)' }}>
            <Icon name="plus" size={14} stroke={2.4} /> Novo investimento
          </button>
        </div>
        <div className="mv-invhead" style={{ display: 'grid', gridTemplateColumns: '1.6fr 1fr 1fr 1fr 0.85fr 90px', gap: 14, padding: '11px 22px', background: 'var(--surface-2)', borderBottom: '1px solid var(--line)' }}>
          {['Ativo','Investido','Valor bruto','Rendimento','Taxa','Vencimento'].map((h,i)=>(
            <div key={h} className="eyebrow" style={{ fontSize: 9, textAlign: i>=1 && i<=3 ? 'right' : 'left' }}>{h}</div>
          ))}
        </div>
        {computed.map(inv => (
          <div key={inv.id} className="mv-invrow" style={{ display: 'grid', gridTemplateColumns: '1.6fr 1fr 1fr 1fr 0.85fr 90px', gap: 14, alignItems: 'center', padding: '14px 22px', borderBottom: '1px solid var(--line)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, minWidth: 0 }}>
              <span style={{ width: 36, height: 36, borderRadius: 10, background: (INV_TYPE_COLOR[inv.type]||'#99A1B0')+'1f', color: INV_TYPE_COLOR[inv.type]||'#99A1B0', display: 'grid', placeItems: 'center', flexShrink: 0 }}>
                <Icon name="trending" size={16} />
              </span>
              <div style={{ minWidth: 0 }}>
                <div style={{ fontSize: 13.5, fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{inv.name}</div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 2 }}>
                  <span className="mono" style={{ fontSize: 10, color: 'var(--ink-4)' }}>{INV_TYPE_LABEL[inv.type]}</span>
                  {inv.exempt && <span className="pill" style={{ fontSize: 8.5, padding: '1px 6px', background: 'var(--pos-bg)', color: 'var(--pos-ink)' }}>Isento IR</span>}
                </div>
              </div>
            </div>
            <div className="num mv-hide-md" style={{ textAlign: 'right', fontSize: 13, color: 'var(--ink-2)' }}>{MV.fmtBRL(inv.invested, {cents:false})}</div>
            <div className="num" style={{ textAlign: 'right', fontSize: 13.5, fontWeight: 700 }}>{MV.fmtBRL(inv.gross, {cents:false})}</div>
            <div style={{ textAlign: 'right' }}>
              <div className="num money-pos" style={{ fontSize: 13.5, fontWeight: 700, color: 'var(--pos-ink)' }}>+{MV.fmtBRL(inv.c.grossYield, {cents:false})}</div>
              <div className="mono" style={{ fontSize: 10.5, color: 'var(--pos-ink)' }}>{MV.fmtSignedPct(inv.c.grossPct)}</div>
            </div>
            <div className="mono mv-hide-sm" style={{ fontSize: 11.5, color: 'var(--ink-2)', fontWeight: 600 }}>{fmtRate(inv)}</div>
            <div>
              <div className="num" style={{ fontSize: 12, fontWeight: 600, color: 'var(--ink-2)' }}>{MV.fmtDate(inv.maturity)}</div>
              <div className="mono" style={{ fontSize: 10, color: 'var(--ink-4)', marginTop: 1 }}>
                {inv.liquidity === 'DAILY' ? 'Diária' : inv.liquidity === 'AT_MATURITY' ? 'No venc.' : 'Programada'}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

window.Investments = Investments;
