/* Formatters — money stored as centavos (Long), formatted only at render */
window.MV = window.MV || {};

MV.fmtBRL = (centavos, opts = {}) => {
  const v = (centavos || 0) / 100;
  const s = v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: opts.cents === false ? 0 : 2, maximumFractionDigits: opts.cents === false ? 0 : 2 });
  return s;
};

// split into "R$ 12.480" + ",90" so we can style the cents smaller
MV.fmtBRLParts = (centavos) => {
  const neg = centavos < 0;
  const abs = Math.abs(centavos || 0);
  const full = (abs / 100).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  const [int, dec] = full.split(',');
  return { sign: neg ? '−' : '', symbol: 'R$', int, dec };
};

MV.fmtNum = (n, d = 0) => (n || 0).toLocaleString('pt-BR', { minimumFractionDigits: d, maximumFractionDigits: d });

MV.fmtPct = (n, d = 1) => `${n >= 0 ? '' : ''}${(n || 0).toLocaleString('pt-BR', { minimumFractionDigits: d, maximumFractionDigits: d })}%`;

MV.fmtSignedPct = (n, d = 1) => `${n > 0 ? '+' : n < 0 ? '−' : ''}${Math.abs(n).toLocaleString('pt-BR', { minimumFractionDigits: d, maximumFractionDigits: d })}%`;

const MESES = ['jan','fev','mar','abr','mai','jun','jul','ago','set','out','nov','dez'];
const MESES_FULL = ['Janeiro','Fevereiro','Março','Abril','Maio','Junho','Julho','Agosto','Setembro','Outubro','Novembro','Dezembro'];

MV.fmtDate = (iso) => {
  const d = new Date(iso + 'T00:00:00');
  return `${String(d.getDate()).padStart(2,'0')}/${String(d.getMonth()+1).padStart(2,'0')}/${d.getFullYear()}`;
};
MV.fmtDateShort = (iso) => {
  const d = new Date(iso + 'T00:00:00');
  return `${String(d.getDate()).padStart(2,'0')} ${MESES[d.getMonth()]}`;
};
MV.fmtDayMonth = (iso) => {
  const d = new Date(iso + 'T00:00:00');
  return { day: String(d.getDate()).padStart(2,'0'), mon: MESES[d.getMonth()] };
};
MV.MESES = MESES;
MV.MESES_FULL = MESES_FULL;

MV.relDays = (n) => n === 0 ? 'hoje' : n === 1 ? 'amanhã' : `em ${n} dias`;
