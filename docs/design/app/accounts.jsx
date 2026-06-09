/* Accounts & Transactions */

const METHOD_LABEL = {
  PIX: 'PIX', TED: 'TED', DOC: 'DOC', DEBIT_CARD: 'Débito', CREDIT_CARD: 'Crédito',
  BOLETO: 'Boleto', CASH: 'Dinheiro', TRANSFER: 'Transferência',
};
const ACCT_TYPE = { CHECKING: 'Conta Corrente', SAVINGS: 'Poupança', CASH: 'Dinheiro', DIGITAL: 'Conta Digital' };

function AccountCard({ a }) {
  const spark = { ac1:[3,4,4,5,6,5,7,8], ac2:[6,5,5,4,5,4,5,4], ac3:[2,3,3,4,4,5,5,6], ac4:[5,5,4,4,5,4,4,3] }[a.id];
  return (
    <div className="card" style={{ padding: '18px 20px', position: 'relative', overflow: 'hidden' }}>
      <div style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: 3, background: a.color }} />
      <div style={{ display: 'flex', alignItems: 'center', gap: 11, marginBottom: 16 }}>
        <div style={{ width: 36, height: 36, borderRadius: 10, background: a.color+'22', color: a.color, display: 'grid', placeItems: 'center', fontWeight: 700, fontSize: 13 }}>
          {a.name.slice(0,2).toUpperCase()}
        </div>
        <div style={{ minWidth: 0 }}>
          <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--ink)' }}>{a.name}</div>
          <div style={{ fontSize: 11.5, color: 'var(--ink-4)' }}>{ACCT_TYPE[a.type]}</div>
        </div>
        <Icon name="dots" size={16} style={{ marginLeft: 'auto', color: 'var(--ink-4)' }} />
      </div>
      <div className="eyebrow" style={{ fontSize: 9.5 }}>Saldo atual</div>
      <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', marginTop: 5 }}>
        <div className="num" style={{ fontSize: 21, fontWeight: 700, letterSpacing: '-.02em' }}>{MV.fmtBRL(a.balance)}</div>
        <Sparkline data={spark} color={a.color} width={64} height={26} />
      </div>
    </div>
  );
}

function TxRow({ t }) {
  const cat = MV.data.catMap[t.categoryId];
  const isCredit = t.type === 'CREDIT';
  const d = MV.fmtDayMonth(t.date);
  return (
    <div className="mv-txrow" style={{ display: 'grid', gridTemplateColumns: '52px 1fr 150px 120px 130px', alignItems: 'center', gap: 14, padding: '13px 18px', borderBottom: '1px solid var(--line)' }}>
      <div style={{ textAlign: 'center' }}>
        <div className="num" style={{ fontSize: 16, fontWeight: 700, lineHeight: 1 }}>{d.day}</div>
        <div className="mono" style={{ fontSize: 9.5, color: 'var(--ink-4)', textTransform: 'uppercase' }}>{d.mon}</div>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, minWidth: 0 }}>
        <span style={{ width: 34, height: 34, borderRadius: 10, background: cat.color+'1c', color: cat.color, display: 'grid', placeItems: 'center', flexShrink: 0 }}>
          <Icon name={cat.icon} size={16} />
        </span>
        <div style={{ minWidth: 0 }}>
          <div style={{ fontSize: 13.5, fontWeight: 600, color: 'var(--ink)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', display: 'flex', alignItems: 'center', gap: 7 }}>
            {t.description}
            {t.recurring && <span title="Recorrente" style={{ color: 'var(--frost-soft)', display: 'inline-flex' }}><Icon name="repeat" size={12} /></span>}
            {t.installment && <span className="mono" style={{ fontSize: 9.5, fontWeight: 600, color: 'var(--warn)', background: 'var(--warn-bg)', padding: '1px 6px', borderRadius: 5 }}>{t.installment}</span>}
          </div>
          <div style={{ fontSize: 11.5, color: 'var(--ink-4)' }}>{cat.name}</div>
        </div>
      </div>
      <div className="mv-hide-md">
        <span className="pill" style={{ background: 'var(--surface-3)', color: 'var(--ink-3)' }}>
          {t.paymentMethod === 'PIX' && <Icon name="pix" size={11} />}
          {METHOD_LABEL[t.paymentMethod]}
        </span>
      </div>
      <div className="mv-hide-sm" style={{ fontSize: 12.5, color: 'var(--ink-3)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{t.source}</div>
      <div className={'num ' + (isCredit ? 'money-pos' : '')} style={{ textAlign: 'right', fontSize: 14.5, fontWeight: 700, color: isCredit ? 'var(--pos-ink)' : 'var(--ink)' }}>
        {isCredit ? '+ ' : '− '}{MV.fmtBRL(t.amount)}
      </div>
    </div>
  );
}

function Accounts() {
  const [filter, setFilter] = React.useState('all');
  const [type, setType] = React.useState('all');
  const total = MV.data.accounts.reduce((s,a)=>s+a.balance,0);

  let txs = MV.data.transactions;
  if (filter !== 'all') txs = txs.filter(t => t.source === filter);
  if (type !== 'all') txs = txs.filter(t => t.type === type);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      {/* accounts strip */}
      <div className="fade-in">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 12 }}>
            <h2 style={{ fontSize: 15, fontWeight: 700, whiteSpace: 'nowrap' }}>Minhas Contas</h2>
            <span style={{ fontSize: 13, color: 'var(--ink-3)', whiteSpace: 'nowrap' }}>Total <span className="num" style={{ fontWeight: 700, color: 'var(--ink)' }}>{MV.fmtBRL(total)}</span></span>
          </div>
          <button className="mono" style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, fontWeight: 600, color: 'var(--frost-deep)', padding: '7px 12px', border: '1px solid var(--accent-line)', borderRadius: 9, background: 'var(--accent-bg)' }}>
            <Icon name="plus" size={14} stroke={2.4} /> Nova conta
          </button>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(230px, 1fr))', gap: 14 }}>
          {MV.data.accounts.map(a => <AccountCard key={a.id} a={a} />)}
        </div>
      </div>

      {/* transaction feed */}
      <div className="card fade-in" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '18px 20px', borderBottom: '1px solid var(--line)', flexWrap: 'wrap' }}>
          <h3 style={{ fontSize: 15, fontWeight: 700 }}>Atividade</h3>
          <div style={{ flex: 1 }} />
          <div style={{ display: 'flex', gap: 3, padding: 3, background: 'var(--surface-2)', borderRadius: 9, border: '1px solid var(--line)' }}>
            {[['all','Tudo'],['CREDIT','Entradas'],['DEBIT','Saídas']].map(([k,l]) => (
              <button key={k} onClick={()=>setType(k)} style={{ padding: '6px 13px', borderRadius: 7, fontSize: 12.5, fontWeight: 600,
                color: type===k?'var(--frost-deep)':'var(--ink-4)', background: type===k?'var(--surface)':'transparent', boxShadow: type===k?'var(--sh-sm)':'none' }}>{l}</button>
            ))}
          </div>
          <button style={{ display: 'flex', alignItems: 'center', gap: 7, padding: '8px 13px', border: '1px solid var(--line)', borderRadius: 9, fontSize: 12.5, fontWeight: 600, color: 'var(--ink-2)', background: 'var(--surface)' }}>
            <Icon name="filter" size={14} /> Filtros
          </button>
          <button style={{ display: 'flex', alignItems: 'center', gap: 7, padding: '8px 13px', border: '1px solid var(--line)', borderRadius: 9, fontSize: 12.5, fontWeight: 600, color: 'var(--ink-2)', background: 'var(--surface)' }}>
            <Icon name="upload" size={14} /> Importar
          </button>
        </div>
        {/* account filter chips */}
        <div style={{ display: 'flex', gap: 8, padding: '12px 20px', borderBottom: '1px solid var(--line)', overflowX: 'auto' }}>
          {[['all','Todas as contas']].concat(MV.data.accounts.map(a=>[a.name,a.name])).map(([k,l]) => (
            <button key={k} onClick={()=>setFilter(k)} className="mono" style={{ whiteSpace: 'nowrap', padding: '6px 12px', borderRadius: 99, fontSize: 11.5, fontWeight: 600,
              border: '1px solid '+(filter===k?'var(--accent-line)':'var(--line)'),
              color: filter===k?'var(--frost-deep)':'var(--ink-3)', background: filter===k?'var(--accent-bg)':'var(--surface)' }}>{l}</button>
          ))}
        </div>
        {/* header row */}
        <div className="mv-txhead" style={{ display: 'grid', gridTemplateColumns: '52px 1fr 150px 120px 130px', gap: 14, padding: '10px 18px', borderBottom: '1px solid var(--line)', background: 'var(--surface-2)' }}>
          {['Data','Descrição','Método','Origem','Valor'].map((h,i)=>(
            <div key={h} className="eyebrow" style={{ fontSize: 9.5, textAlign: i===0?'center':i===4?'right':'left' }}>{h}</div>
          ))}
        </div>
        <div>
          {txs.map(t => <TxRow key={t.id} t={t} />)}
          {txs.length === 0 && <div style={{ padding: 40, textAlign: 'center', color: 'var(--ink-4)', fontSize: 13 }}>Nenhuma transação encontrada.</div>}
        </div>
      </div>
    </div>
  );
}

window.Accounts = Accounts;
