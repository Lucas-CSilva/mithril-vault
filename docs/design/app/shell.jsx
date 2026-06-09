/* App shell — sidebar nav + topbar. Exports Shell, NAV, Logo to window. */

const NAV = [
  { id: 'dashboard',     label: 'Visão Geral',    icon: 'grid' },
  { id: 'accounts',      label: 'Contas',         icon: 'wallet' },
  { id: 'cards',         label: 'Cartões',        icon: 'card' },
  { id: 'planning',      label: 'Planejamento',   icon: 'target' },
  { id: 'investments',   label: 'Investimentos',  icon: 'trending' },
  { id: 'subscriptions', label: 'Assinaturas',    icon: 'repeat' },
];

const PAGE_META = {
  dashboard:     { title: 'Visão Geral',    sub: 'Sua saúde financeira em tempo real' },
  accounts:      { title: 'Contas & Transações', sub: 'Todo movimento de dinheiro, em um só lugar' },
  cards:         { title: 'Cartões',        sub: 'Limites, faturas e ciclos de cobrança' },
  planning:      { title: 'Planejamento',   sub: 'Orçamentos e cofres de objetivos' },
  investments:   { title: 'Investimentos',  sub: 'Carteira de renda fixa' },
  subscriptions: { title: 'Assinaturas',    sub: 'Sua economia recorrente sob controle' },
};

function Logo({ size = 26, withText = true }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
      <div style={{ width: size, height: size, position: 'relative', flexShrink: 0 }}>
        <svg width={size} height={size} viewBox="0 0 32 32" fill="none">
          <rect x="16" y="2.5" width="19" height="19" rx="3.5" transform="rotate(45 16 2.5)"
            fill="var(--frost-deep)" />
          <rect x="16" y="8.7" width="10.3" height="10.3" rx="2" transform="rotate(45 16 8.7)"
            fill="none" stroke="#fff" strokeWidth="1.5" strokeOpacity="0.85" />
          <line x1="16" y1="2.5" x2="16" y2="29.5" stroke="#fff" strokeWidth="1.2" strokeOpacity="0.35" />
        </svg>
      </div>
      {withText && (
        <div style={{ lineHeight: 1.05 }}>
          <div style={{ fontWeight: 700, fontSize: 15.5, letterSpacing: '-.015em', color: 'var(--ink)', whiteSpace: 'nowrap' }}>Mithril Vault</div>
          <div className="mono" style={{ fontSize: 9, letterSpacing: '.16em', color: 'var(--ink-4)', textTransform: 'uppercase', marginTop: 2 }}>Finanças</div>
        </div>
      )}
    </div>
  );
}

function NavItem({ item, active, compact, onClick }) {
  const [hover, setHover] = React.useState(false);
  const on = active === item.id;
  return (
    <button
      onClick={() => onClick(item.id)}
      onMouseEnter={() => setHover(true)} onMouseLeave={() => setHover(false)}
      title={compact ? item.label : undefined}
      style={{
        display: 'flex', alignItems: 'center', gap: 12, width: '100%',
        padding: compact ? '11px' : '9px 12px',
        justifyContent: compact ? 'center' : 'flex-start',
        borderRadius: 11, position: 'relative',
        color: on ? 'var(--frost-deep)' : hover ? 'var(--ink)' : 'var(--ink-3)',
        background: on ? 'var(--accent-bg)' : hover ? 'var(--surface-2)' : 'transparent',
        fontWeight: on ? 600 : 500, fontSize: 14, transition: 'all .15s',
      }}>
      {on && !compact && <span style={{ position:'absolute', left:-12, top:'50%', transform:'translateY(-50%)', width:3, height:18, borderRadius:99, background:'var(--frost-deep)' }} />}
      <Icon name={item.icon} size={19} stroke={on ? 2.3 : 2} />
      {!compact && <span>{item.label}</span>}
    </button>
  );
}

function Sidebar({ route, setRoute, compact, mobileOpen, setMobileOpen }) {
  return (
    <aside style={{
      width: compact ? 76 : 'var(--sidebar-w)', flexShrink: 0,
      background: 'var(--surface)', borderRight: '1px solid var(--line)',
      display: 'flex', flexDirection: 'column', height: '100%',
      padding: compact ? '20px 14px' : '22px 18px',
      position: 'relative', zIndex: 30,
      transition: 'width .2s',
    }}>
      <div style={{ padding: compact ? '0 0 22px' : '0 6px 24px', display:'flex', justifyContent: compact ? 'center' : 'flex-start' }}>
        <Logo withText={!compact} size={compact ? 28 : 26} />
      </div>

      <nav style={{ display: 'flex', flexDirection: 'column', gap: 4, flex: 1 }}>
        {!compact && <div className="eyebrow" style={{ padding: '4px 12px 8px' }}>Menu</div>}
        {NAV.map(item => (
          <NavItem key={item.id} item={item} active={route} compact={compact}
            onClick={(id) => { setRoute(id); setMobileOpen && setMobileOpen(false); }} />
        ))}
      </nav>

      {/* user card */}
      <div style={{ marginTop: 'auto', paddingTop: 16, borderTop: '1px solid var(--line)' }}>
        <button style={{
          display: 'flex', alignItems: 'center', gap: 10, width: '100%',
          padding: compact ? 6 : '8px 8px', borderRadius: 12,
          justifyContent: compact ? 'center' : 'flex-start',
        }}>
          <div style={{ width: 34, height: 34, borderRadius: 10, flexShrink: 0,
            background: 'linear-gradient(135deg, var(--frost-soft), var(--frost-deep))',
            color: '#fff', display: 'grid', placeItems: 'center', fontWeight: 700, fontSize: 13 }}>RA</div>
          {!compact && (
            <div style={{ textAlign: 'left', minWidth: 0, flex: 1 }}>
              <div style={{ fontWeight: 600, fontSize: 13, color: 'var(--ink)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>Rafael Almeida</div>
              <div className="mono" style={{ fontSize: 10, color: 'var(--ink-4)' }}>rafael@email.com</div>
            </div>
          )}
          {!compact && <Icon name="settings" size={16} style={{ color: 'var(--ink-4)' }} />}
        </button>
      </div>
    </aside>
  );
}

function Topbar({ route, onMenu, month, setMonth }) {
  const meta = PAGE_META[route] || PAGE_META.dashboard;
  return (
    <header style={{
      display: 'flex', alignItems: 'center', gap: 16, padding: '18px 30px',
      borderBottom: '1px solid var(--line)', background: 'rgba(243,245,249,0.82)',
      backdropFilter: 'blur(12px)', position: 'sticky', top: 0, zIndex: 20,
    }}>
      <button className="mv-menu-btn" onClick={onMenu} style={{ display: 'none', color: 'var(--ink-2)' }}>
        <Icon name="menu" size={22} />
      </button>
      <div style={{ minWidth: 0 }}>
        <h1 style={{ fontSize: 21, fontWeight: 700, letterSpacing: '-.02em', lineHeight: 1.1, color: 'var(--ink)', whiteSpace: 'nowrap' }}>{meta.title}</h1>
        <div className="mv-topbar-sub" style={{ fontSize: 12.5, color: 'var(--ink-3)', marginTop: 1, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{meta.sub}</div>
      </div>

      <div style={{ flex: 1 }} />

      <div className="mv-search" style={{
        display: 'flex', alignItems: 'center', gap: 9, padding: '9px 14px',
        background: 'var(--surface)', border: '1px solid var(--line)', borderRadius: 11,
        color: 'var(--ink-4)', width: 230,
      }}>
        <Icon name="search" size={16} />
        <span style={{ fontSize: 13.5 }}>Buscar transações…</span>
        <span className="mono" style={{ marginLeft: 'auto', fontSize: 10, padding: '2px 6px', background: 'var(--surface-3)', borderRadius: 5, color: 'var(--ink-4)' }}>⌘K</span>
      </div>

      <button className="mv-month" style={{
        display: 'flex', alignItems: 'center', gap: 9, padding: '9px 13px',
        background: 'var(--surface)', border: '1px solid var(--line)', borderRadius: 11,
        color: 'var(--ink-2)', fontWeight: 600, fontSize: 13.5,
      }}>
        <Icon name="calendar" size={16} style={{ color: 'var(--ink-4)' }} />
        Junho 2026
        <Icon name="chevDown" size={15} style={{ color: 'var(--ink-4)' }} />
      </button>

      <button style={{
        position: 'relative', width: 40, height: 40, borderRadius: 11,
        background: 'var(--surface)', border: '1px solid var(--line)',
        display: 'grid', placeItems: 'center', color: 'var(--ink-2)',
      }}>
        <Icon name="bell" size={18} />
        <span style={{ position: 'absolute', top: 9, right: 10, width: 7, height: 7, borderRadius: 99, background: 'var(--neg)', border: '1.5px solid var(--surface)' }} />
      </button>

      <button style={{
        display: 'flex', alignItems: 'center', gap: 8, padding: '0 16px', height: 40,
        background: 'var(--frost-deep)', color: '#fff', borderRadius: 11,
        fontWeight: 600, fontSize: 13.5, boxShadow: 'var(--sh-sm)',
      }}>
        <Icon name="plus" size={17} stroke={2.4} />
        <span className="mv-add-label">Adicionar</span>
      </button>
    </header>
  );
}

function Shell({ route, setRoute, navLayout, children }) {
  const [mobileOpen, setMobileOpen] = React.useState(false);
  const compact = navLayout === 'compact';

  return (
    <div className="mv-shell" style={{ display: 'flex', height: '100%', overflow: 'hidden' }}>
      {/* desktop sidebar */}
      <div className="mv-sidebar-wrap">
        <Sidebar route={route} setRoute={setRoute} compact={compact} />
      </div>

      {/* mobile drawer */}
      {mobileOpen && (
        <div className="mv-drawer" onClick={() => setMobileOpen(false)}
          style={{ position: 'fixed', inset: 0, zIndex: 60, background: 'rgba(46,52,64,.4)', backdropFilter: 'blur(2px)' }}>
          <div onClick={(e) => e.stopPropagation()} style={{ width: 'var(--sidebar-w)', height: '100%' }}>
            <Sidebar route={route} setRoute={setRoute} compact={false} setMobileOpen={setMobileOpen} />
          </div>
        </div>
      )}

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0, height: '100%' }}>
        <Topbar route={route} onMenu={() => setMobileOpen(true)} />
        <main style={{ flex: 1, overflowY: 'auto', overflowX: 'hidden' }}>
          <div style={{ maxWidth: 1320, margin: '0 auto', padding: '26px 30px 60px' }}>
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}

Object.assign(window, { Shell, NAV, PAGE_META, Logo });
