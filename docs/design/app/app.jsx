/* App root — routing + tweaks */

const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
  "accent": "Azul-tinta",
  "navLayout": "Expandida",
  "density": "Normal"
}/*EDITMODE-END*/;

// Acento — quiet-luxury options drawn from the brand families (no green)
const ACCENTS = {
  'Azul-tinta': { deep: '#3C5070', bg: '#EAEDF2', line: '#D6DBE3' },
  'Ardósia':    { deep: '#566B82', bg: '#EAEEF1', line: '#D5DDE3' },
  'Vinho':      { deep: '#7C3A49', bg: '#F2E9EB', line: '#E3D1D5' },
};

function ComingSoon({ route }) {
  const meta = PAGE_META[route];
  return (
    <div className="card fade-in" style={{ padding: '60px', display: 'grid', placeItems: 'center', textAlign: 'center', minHeight: 360 }}>
      <div>
        <div style={{ width: 56, height: 56, borderRadius: 16, background: 'var(--accent-bg)', color: 'var(--frost-deep)', display: 'grid', placeItems: 'center', margin: '0 auto 16px' }}>
          <Icon name={NAV.find(n => n.id === route)?.icon || 'grid'} size={26} />
        </div>
        <h2 style={{ fontSize: 18, fontWeight: 700 }}>{meta.title}</h2>
        <p style={{ color: 'var(--ink-3)', marginTop: 6, fontSize: 14 }}>Em construção…</p>
      </div>
    </div>
  );
}

const SCREENS = {
  dashboard:     () => <Dashboard />,
  accounts:      () => window.Accounts     ? <Accounts />     : <ComingSoon route="accounts" />,
  cards:         () => window.Cards        ? <Cards />        : <ComingSoon route="cards" />,
  planning:      () => window.Planning     ? <Planning />     : <ComingSoon route="planning" />,
  investments:   () => window.Investments  ? <Investments />  : <ComingSoon route="investments" />,
  subscriptions: () => window.Subscriptions ? <Subscriptions /> : <ComingSoon route="subscriptions" />,
};

function App() {
  const [t, setTweak] = useTweaks(TWEAK_DEFAULTS);

  // Mithril is the committed direction; tweaks vary the accent within it.
  React.useEffect(() => {
    document.documentElement.setAttribute('data-theme', 'mithril');
  }, []);

  React.useEffect(() => {
    const a = ACCENTS[t.accent] || ACCENTS['Azul-tinta'];
    const r = document.documentElement.style;
    r.setProperty('--frost-deep', a.deep);
    r.setProperty('--accent-bg', a.bg);
    r.setProperty('--accent-line', a.line);
  }, [t.accent]);

  const [route, setRoute] = React.useState(() => {
    const h = location.hash.replace('#', '');
    if (SCREENS[h]) return h;
    const saved = localStorage.getItem('mv_route');
    return SCREENS[saved] ? saved : 'dashboard';
  });

  React.useEffect(() => {
    const onHash = () => {
      const h = location.hash.replace('#', '');
      if (SCREENS[h]) setRoute(h);
    };
    window.addEventListener('hashchange', onHash);
    return () => window.removeEventListener('hashchange', onHash);
  }, []);

  const go = (r) => {
    setRoute(r);
    location.hash = r;
    localStorage.setItem('mv_route', r);
    const m = document.querySelector('main');
    if (m) m.scrollTop = 0;
  };

  const render = SCREENS[route] || SCREENS.dashboard;

  return (
    <Shell route={route} setRoute={go} theme="mithril" navLayout={t.navLayout === 'Compacta' ? 'compact' : 'full'}>
      {render()}
      <TweaksPanel>
        <TweakSection label="Acento" />
        <TweakRadio label="Cor" value={t.accent}
          options={['Azul-tinta', 'Ardósia', 'Vinho']}
          onChange={(v) => setTweak('accent', v)} />
        <TweakSection label="Navegação" />
        <TweakRadio label="Sidebar" value={t.navLayout}
          options={['Expandida', 'Compacta']}
          onChange={(v) => setTweak('navLayout', v)} />
        <TweakSection label="Densidade" />
        <TweakRadio label="Espaçamento" value={t.density}
          options={['Denso', 'Normal', 'Espaçado']}
          onChange={(v) => {
            setTweak('density', v);
            const map = { 'Denso': '22px 24px 50px', 'Normal': '26px 30px 60px', 'Espaçado': '34px 36px 80px' };
            document.querySelector('main > div').style.padding = map[v];
          }} />
      </TweaksPanel>
    </Shell>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
