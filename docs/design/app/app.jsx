/* App root — routing + tweaks */

const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
  "navLayout": "Expandida",
  "accent": "#5E81AC",
  "density": "Normal"
}/*EDITMODE-END*/;

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

  // apply accent override
  React.useEffect(() => {
    document.documentElement.style.setProperty('--frost-deep', t.accent);
    const hex = t.accent;
    document.documentElement.style.setProperty('--accent-bg', hex + '18');
    document.documentElement.style.setProperty('--accent-line', hex + '40');
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
    <Shell route={route} setRoute={go} navLayout={t.navLayout === 'Compacta' ? 'compact' : 'full'}>
      {render()}
      <TweaksPanel>
        <TweakSection label="Navegação" />
        <TweakRadio label="Sidebar" value={t.navLayout}
          options={['Expandida', 'Compacta']}
          onChange={(v) => setTweak('navLayout', v)} />
        <TweakSection label="Cor de destaque" />
        <TweakColor label="Accent" value={t.accent}
          options={['#5E81AC', '#88C0D0', '#8FBCBB', '#7E9F69', '#A98AA3']}
          onChange={(v) => setTweak('accent', v)} />
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
