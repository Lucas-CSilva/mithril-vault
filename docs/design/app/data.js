/* Mithril Vault — mock dataset. All money in CENTAVOS (Long). pt-BR. */
window.MV = window.MV || {};

MV.data = (() => {
  // ---- Categories (system) -------------------------------------
  const cat = (id, name, icon, color, parent = null) => ({ id, name, icon, color, parentId: parent });
  const categories = [
    cat('alim', 'Alimentação', 'cart', '#B0795F'),
    cat('alim-merc', 'Supermercado', 'cart', '#B0795F', 'alim'),
    cat('alim-rest', 'Restaurante', 'utensils', '#B0795F', 'alim'),
    cat('alim-deliv', 'Delivery', 'bag', '#B0795F', 'alim'),
    cat('mora', 'Moradia', 'home', '#3C5070'),
    cat('mora-alug', 'Aluguel', 'home', '#3C5070', 'mora'),
    cat('mora-ener', 'Energia', 'bolt', '#3C5070', 'mora'),
    cat('trans', 'Transporte', 'car', '#5E7A96'),
    cat('trans-comb', 'Combustível', 'fuel', '#5E7A96', 'trans'),
    cat('trans-app', 'Aplicativo', 'car', '#5E7A96', 'trans'),
    cat('saude', 'Saúde', 'heart', '#8E3A4B'),
    cat('educ', 'Educação', 'book', '#3E6B82'),
    cat('lazer', 'Lazer', 'sparkle', '#9A8AA3'),
    cat('vest', 'Vestuário', 'shirt', '#A87E84'),
    cat('serv', 'Serviços & Assinaturas', 'repeat', '#9E7A4E'),
    cat('serv-stream', 'Streaming', 'play', '#9E7A4E', 'serv'),
    cat('inv', 'Investimentos', 'trending', '#6E7E96'),
    cat('transf', 'Transferências', 'swap', '#8A93A3'),
    cat('renda', 'Renda', 'arrow-down-left', '#4E7C66'),
    cat('outros', 'Outros', 'dots', '#9298A2'),
  ];
  const catMap = Object.fromEntries(categories.map(c => [c.id, c]));

  // ---- Accounts ------------------------------------------------
  const accounts = [
    { id: 'ac1', name: 'Nubank',          type: 'DIGITAL',  institution: 'Nu Pagamentos', color: '#7E6A86', balance: 1284090, initial: 980000 },
    { id: 'ac2', name: 'Itaú Corrente',   type: 'CHECKING', institution: 'Itaú',          color: '#B0795F', balance: 742355,  initial: 600000 },
    { id: 'ac3', name: 'Inter',           type: 'DIGITAL',  institution: 'Banco Inter',   color: '#9E7A4E', balance: 318800,  initial: 250000 },
    { id: 'ac4', name: 'Carteira',        type: 'CASH',     institution: null,            color: '#6E7E96', balance: 43000,   initial: 50000 },
  ];

  // ---- Credit cards + invoices --------------------------------
  const cards = [
    { id: 'cc1', name: 'Nubank Ultravioleta', institution: 'Nubank', last4: '4417', cardType: 'PHYSICAL', limit: 1200000, used: 487340, closingDay: 3, dueDay: 10, color: '#3B2150', account: 'ac1' },
    { id: 'cc2', name: 'Itaú Click',          institution: 'Itaú',   last4: '9082', cardType: 'PHYSICAL', limit: 800000,  used: 213560, closingDay: 15, dueDay: 22, color: '#C2410C', account: 'ac2' },
    { id: 'cc3', name: 'Inter Black',         institution: 'Inter',  last4: '7765', cardType: 'VIRTUAL',  limit: 500000,  used: 94200,  closingDay: 28, dueDay: 5,  color: '#1F2937', account: 'ac3' },
  ];

  // invoices for the cycle monitor (cc1)
  const invoices = [
    { id: 'inv-m2', cardId: 'cc1', ref: '2026-04', status: 'PAID',   total: 512880, closing: '2026-04-03', due: '2026-04-10' },
    { id: 'inv-m1', cardId: 'cc1', ref: '2026-05', status: 'PAID',   total: 468120, closing: '2026-05-03', due: '2026-05-10' },
    { id: 'inv-c',  cardId: 'cc1', ref: '2026-06', status: 'OPEN',   total: 487340, closing: '2026-06-03', due: '2026-06-10' },
    { id: 'inv-n',  cardId: 'cc1', ref: '2026-07', status: 'OPEN',   total: 86400,  closing: '2026-07-03', due: '2026-07-10' },
  ];

  // ---- Transactions (recent feed) -----------------------------
  // type DEBIT/CREDIT, amount positive centavos
  const tx = (id, date, desc, amount, type, catId, method, src, opts = {}) =>
    ({ id, date, description: desc, amount, type, categoryId: catId, paymentMethod: method, source: src, ...opts });

  const transactions = [
    tx('t01','2026-06-06','Supermercado Pão de Açúcar', 38790, 'DEBIT','alim-merc','DEBIT_CARD','Nubank'),
    tx('t02','2026-06-06','iFood • Almoço',            6480,  'DEBIT','alim-deliv','CREDIT_CARD','Nubank Ultravioleta'),
    tx('t03','2026-06-05','Salário — Mensal',          1250000,'CREDIT','renda','TED','Itaú Corrente', { recurring:true }),
    tx('t04','2026-06-05','Posto Shell',               24500, 'DEBIT','trans-comb','CREDIT_CARD','Itaú Click'),
    tx('t05','2026-06-04','Netflix',                   5590,  'DEBIT','serv-stream','CREDIT_CARD','Nubank Ultravioleta', { recurring:true }),
    tx('t06','2026-06-04','Aluguel — Apto 142',        320000,'DEBIT','mora-alug','BOLETO','Itaú Corrente', { recurring:true }),
    tx('t07','2026-06-03','PIX recebido • Marina',     15000, 'CREDIT','transf','PIX','Nubank'),
    tx('t08','2026-06-03','Uber • Centro',             3120,  'DEBIT','trans-app','CREDIT_CARD','Nubank Ultravioleta'),
    tx('t09','2026-06-02','Drogaria São Paulo',        8940,  'DEBIT','saude','DEBIT_CARD','Inter'),
    tx('t10','2026-06-02','Transferência p/ Cofre',    50000, 'DEBIT','transf','TRANSFER','Nubank', { transfer:true }),
    tx('t11','2026-06-01','Restaurante Fasano',        18600, 'DEBIT','alim-rest','CREDIT_CARD','Nubank Ultravioleta'),
    tx('t12','2026-06-01','Enel • Energia',            21870, 'DEBIT','mora-ener','BOLETO','Itaú Corrente', { recurring:true }),
    tx('t13','2026-05-31','Spotify Família',           3490,  'DEBIT','serv-stream','CREDIT_CARD','Nubank Ultravioleta', { recurring:true }),
    tx('t14','2026-05-30','Amazon • Livros',           12730, 'DEBIT','educ','CREDIT_CARD','Inter Black'),
    tx('t15','2026-05-30','Zara',                      27900, 'DEBIT','vest','CREDIT_CARD','Itaú Click', { installment:'1/3' }),
    tx('t16','2026-05-29','PIX • Freelance design',    180000,'CREDIT','renda','PIX','Nubank'),
    tx('t17','2026-05-28','Carrefour',                 15640, 'DEBIT','alim-merc','DEBIT_CARD','Nubank'),
    tx('t18','2026-05-28','Cinema Cinemark',           4800,  'DEBIT','lazer','CREDIT_CARD','Nubank Ultravioleta'),
  ];

  // ---- Cash-flow series (liquid balance EOD) ------------------
  // last 30 days, centavos
  const cashflow30 = (() => {
    // smooth upward drift from ~20.5k to the hero balance, with gentle noise.
    // noise tapers to 0 at the end so the line lands softly (no final spike).
    const start = 2050000, end = 2388245;
    const noise = [0,-9000,-4000,6000,14000,8000,-3000,-12000,-6000,4000,
                   12000,9000,-2000,-10000,-15000,-8000,2000,11000,16000,9000,
                   -1000,-8000,-14000,-7000,3000,10000,6000,-2000,0,0];
    const pts = [];
    for (let i = 0; i < 30; i++) {
      const base = start + (end - start) * (i / 29);
      pts.push(Math.round(base + noise[i]));
    }
    pts[0] = start; pts[29] = end; // = saldo líquido hero
    return pts;
  })();
  const cashflow7  = cashflow30.slice(-7);
  const cashflow12m = [1680000,1742000,1610000,1890000,1955000,2040000,1980000,2160000,2240000,2190000,2305000,2388245];

  // ---- Expense distribution (current month, by top-level) -----
  const expenseDist = [
    { id:'mora',  label:'Moradia',     value: 561870, color:'#3C5070' },
    { id:'alim',  label:'Alimentação', value: 384200, color:'#B0795F' },
    { id:'trans', label:'Transporte',  value: 198400, color:'#5E7A96' },
    { id:'lazer', label:'Lazer',       value:  96300, color:'#9A8AA3' },
    { id:'serv',  label:'Serviços',    value:  74600, color:'#9E7A4E' },
    { id:'vest',  label:'Vestuário',   value:  62900, color:'#A87E84' },
    { id:'saude', label:'Saúde',       value:  41800, color:'#8E3A4B' },
  ];

  // ---- Obligations radar (next 7 days) ------------------------
  const obligations = [
    { id:'o1', name:'Fatura Nubank Ultravioleta', amount:487340, date:'2026-06-10', days:3, kind:'fatura' },
    { id:'o2', name:'Spotify Família',            amount:3490,   date:'2026-06-09', days:2, kind:'assinatura' },
    { id:'o3', name:'Fatura Inter Black',         amount:94200,  date:'2026-06-05', days:0, kind:'fatura' },
    { id:'o4', name:'Disney+',                    amount:4390,   date:'2026-06-12', days:5, kind:'assinatura' },
    { id:'o5', name:'Academia Smart Fit',         amount:9990,   date:'2026-06-13', days:6, kind:'assinatura' },
  ];

  // ---- Budgets (current month) --------------------------------
  const budgets = [
    { id:'b1', categoryId:'alim',  limit: 450000, spent: 384200 },
    { id:'b2', categoryId:'trans', limit: 200000, spent: 198400 },
    { id:'b3', categoryId:'lazer', limit:  80000, spent:  96300 },
    { id:'b4', categoryId:'serv',  limit: 100000, spent:  74600 },
    { id:'b5', categoryId:'vest',  limit:  60000, spent:  62900 },
    { id:'b6', categoryId:'saude', limit:  80000, spent:  41800 },
  ];

  // ---- Goals (cofres) -----------------------------------------
  const goals = [
    { id:'g1', name:'Reserva de Emergência', icon:'shield',  target: 3000000, current: 2150000, deadline:'2026-12-31', color:'#3C5070' },
    { id:'g2', name:'Viagem Japão',          icon:'plane',   target: 1800000, current:  720000, deadline:'2027-03-01', color:'#5E7A96' },
    { id:'g3', name:'MacBook Pro',           icon:'laptop',  target:  1600000, current: 1440000, deadline:null,        color:'#9A8AA3' },
    { id:'g4', name:'Fundo Carro',           icon:'car',     target: 4500000, current:  980000, deadline:'2028-01-01', color:'#9E7A4E' },
  ];

  // ---- Investments (renda fixa) -------------------------------
  const investments = [
    { id:'i1', name:'CDB Nubank 110% CDI',   type:'CDB',           inst:'Nubank',  invested: 1500000, gross: 1623400, rateType:'CDI_PERCENTAGE', rate:11000, start:'2025-02-10', maturity:'2027-02-10', liquidity:'DAILY',      exempt:false },
    { id:'i2', name:'Tesouro Selic 2029',    type:'TESOURO_SELIC', inst:'Tesouro', invested: 2000000, gross: 2118700, rateType:'SELIC_PERCENTAGE',rate:10000, start:'2024-08-01', maturity:'2029-03-01', liquidity:'DAILY',      exempt:false },
    { id:'i3', name:'LCI Inter 95% CDI',     type:'LCI',           inst:'Inter',   invested: 1000000, gross: 1067200, rateType:'CDI_PERCENTAGE', rate:9500,  start:'2025-06-15', maturity:'2026-12-15', liquidity:'AT_MATURITY',exempt:true },
    { id:'i4', name:'Tesouro IPCA+ 2035',    type:'TESOURO_IPCA',  inst:'Tesouro', invested: 1200000, gross: 1284900, rateType:'IPCA_PLUS',      rate:625,   start:'2024-11-20', maturity:'2035-05-15', liquidity:'AT_MATURITY',exempt:false },
    { id:'i5', name:'CDB Inter 102% CDI',    type:'CDB',           inst:'Inter',   invested:  800000, gross:  842100, rateType:'CDI_PERCENTAGE', rate:10200, start:'2025-09-01', maturity:'2026-09-01', liquidity:'DAILY',      exempt:false },
  ];

  // ---- Subscriptions ------------------------------------------
  const subscriptions = [
    { id:'s1', name:'Netflix',        cat:'serv-stream', amount: 5590,  cycle:'MONTHLY',  next:'2026-06-15', method:'CREDIT_CARD', status:'ACTIVE',  color:'#BF616A' },
    { id:'s2', name:'Spotify Família',cat:'serv-stream', amount: 3490,  cycle:'MONTHLY',  next:'2026-06-09', method:'CREDIT_CARD', status:'ACTIVE',  color:'#5E7A96' },
    { id:'s3', name:'Disney+',        cat:'serv-stream', amount: 4390,  cycle:'MONTHLY',  next:'2026-06-12', method:'CREDIT_CARD', status:'ACTIVE',  color:'#5E81AC' },
    { id:'s4', name:'Amazon Prime',   cat:'serv-stream', amount: 1990,  cycle:'MONTHLY',  next:'2026-06-20', method:'CREDIT_CARD', status:'ACTIVE',  color:'#81A1C1' },
    { id:'s5', name:'Academia Smart Fit', cat:'saude',   amount: 9990,  cycle:'MONTHLY',  next:'2026-06-13', method:'BOLETO',      status:'ACTIVE',  color:'#C5854F' },
    { id:'s6', name:'iCloud 2TB',     cat:'serv',        amount: 4990,  cycle:'MONTHLY',  next:'2026-06-18', method:'CREDIT_CARD', status:'ACTIVE',  color:'#99A1B0' },
    { id:'s7', name:'ChatGPT Plus',   cat:'serv',        amount: 11800, cycle:'MONTHLY',  next:'2026-06-24', method:'CREDIT_CARD', status:'ACTIVE',  color:'#6E7E96' },
    { id:'s8', name:'Adobe CC',       cat:'serv',        amount: 33900, cycle:'ANNUAL',   next:'2026-11-02', method:'CREDIT_CARD', status:'ACTIVE',  color:'#A98AA3' },
    { id:'s9', name:'HBO Max',        cat:'serv-stream', amount: 2990,  cycle:'MONTHLY',  next:null,         method:'CREDIT_CARD', status:'CANCELLED', color:'#A98AA3' },
  ];

  return {
    categories, catMap, accounts, cards, invoices, transactions,
    cashflow7, cashflow30, cashflow12m, expenseDist, obligations,
    budgets, goals, investments, subscriptions,
    // dashboard headline figures
    hero: {
      saldoLiquido: 2388245,        // sum active accts - open invoices
      contas: 2388245 + 487340 + 94200, // gross before open invoices
      openInvoices: 487340 + 94200,
      receitas: 1430000,
      despesas: 689420,
      investidoMes: 150000,
      saldoMes: 1430000 - 689420,
      patrimonio: 2388245 + 6936300 + 5290000, // liquid + investments gross + cofres? (display)
      deltaSaldo: 4.2,
    },
  };
})();
