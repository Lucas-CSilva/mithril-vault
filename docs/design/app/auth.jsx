/* Auth + error pages — shared components.
   Exports LoginPage, RegisterPage, ErrorPage, mountAuth to window.
   Depends on icons.jsx (Icon) + auth.css. */

const { useState } = React;

const APP_URL = 'Mithril%20Vault.html';

/* ---- Light ring-seal logo for the dark brand panel -------- */
function LogoLight({ size = 32 }) {
  return (
    <div className="brand-top">
      <div className="brand-seal" style={{ width: size, height: size, fontSize: size * 0.48 }}>M</div>
      <div className="brand-word">
        <div className="nm">Mithril Vault</div>
        <div className="sub">Finanças pessoais</div>
      </div>
    </div>
  );
}

/* ---- Dark ring-seal logo for light backgrounds (errors) --- */
function LogoMark({ size = 30 }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
      <div className="mark-seal" style={{ width: size, height: size, fontSize: size * 0.48 }}>M</div>
      <div style={{ fontFamily: 'var(--display)', fontWeight: 600, fontSize: 16, color: 'var(--ink)', whiteSpace: 'nowrap' }}>Mithril Vault</div>
    </div>
  );
}

/* ---- Brand panel (left side) ------------------------------ */
function BrandPanel({ eyebrow, head, lede, feats }) {
  return (
    <div className="brand-panel">
      <div className="brand-ring" />
      <div className="brand-ring inner" />
      <LogoLight />

      <div className="brand-body">
        <div className="brand-eyebrow">{eyebrow}</div>
        <h2 className="brand-head">{head}</h2>
        <p className="brand-lede">{lede}</p>
        <div className="brand-feats">
          {feats.map((f, i) => (
            <div className="brand-feat" key={i}>
              <span className="tick"><Icon name="check" size={13} stroke={2.6} /></span>
              <span>{f}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="brand-foot">
        <Icon name="shield" size={15} />
        <span>Seus dados isolados por usuário — visíveis só para você.</span>
      </div>
    </div>
  );
}

/* ---- Field primitives ------------------------------------- */
function Field({ icon, label, type = 'text', value, onChange, onBlur, placeholder, error, autoComplete, name, inputMode, children }) {
  return (
    <div className="auth-field">
      <label htmlFor={name}>{label}</label>
      <div className={'auth-input' + (error ? ' is-error' : '')}>
        {icon && <Icon name={icon} size={18} className="auth-input-icon" />}
        <input
          id={name} name={name} type={type} value={value} placeholder={placeholder}
          autoComplete={autoComplete} inputMode={inputMode}
          onChange={(e) => onChange(e.target.value)} onBlur={onBlur} />
        {children}
      </div>
      {error && <div className="auth-err"><Icon name="alertCircle" size={13} /> {error}</div>}
    </div>
  );
}

function PasswordField({ label, value, onChange, onBlur, error, autoComplete, name, placeholder }) {
  const [show, setShow] = useState(false);
  return (
    <Field icon="lock" label={label} name={name} type={show ? 'text' : 'password'}
      value={value} onChange={onChange} onBlur={onBlur} error={error}
      autoComplete={autoComplete} placeholder={placeholder}>
      <button type="button" className="pw-toggle" tabIndex={-1}
        aria-label={show ? 'Ocultar senha' : 'Mostrar senha'}
        onClick={() => setShow(s => !s)}>
        <Icon name={show ? 'eyeOff' : 'eye'} size={17} />
      </button>
    </Field>
  );
}

function Checkbox({ checked, onChange, children, name }) {
  return (
    <label className="auth-check">
      <input type="checkbox" name={name} checked={checked} onChange={(e) => onChange(e.target.checked)} />
      <span className="box"><Icon name="check" size={12} stroke={3} /></span>
      <span className="txt">{children}</span>
    </label>
  );
}

function SubmitButton({ loading, children, loadingLabel }) {
  return (
    <button type="submit" className="auth-btn" disabled={loading}>
      {loading ? (<><span className="spin" /> {loadingLabel}</>) : children}
    </button>
  );
}

function AuthBanner({ children }) {
  return (
    <div className="auth-banner">
      <Icon name="alertTriangle" size={16} />
      <span>{children}</span>
    </div>
  );
}

/* ---- Password strength ------------------------------------ */
function scorePassword(pw) {
  if (!pw) return 0;
  let s = 0;
  if (pw.length >= 8) s++;
  if (pw.length >= 12) s++;
  if (/[a-z]/.test(pw) && /[A-Z]/.test(pw)) s++;
  if (/\d/.test(pw) && /[^A-Za-z0-9]/.test(pw)) s++;
  return Math.min(s, 4);
}
const STRENGTH = [
  { label: 'Muito fraca', color: 'var(--neg)', hint: 'Use ao menos 8 caracteres' },
  { label: 'Fraca', color: 'var(--neg)', hint: 'Misture maiúsculas e minúsculas' },
  { label: 'Razoável', color: 'var(--warn)', hint: 'Adicione números ou símbolos' },
  { label: 'Boa', color: 'var(--gold)', hint: 'Quase lá' },
  { label: 'Forte', color: 'var(--pos)', hint: 'Senha sólida' },
];
function StrengthMeter({ value }) {
  const score = scorePassword(value);
  const meta = STRENGTH[score];
  return (
    <div className="strength">
      <div className="strength-bars">
        {[0, 1, 2, 3].map(i => (
          <span key={i} className="strength-seg"
            style={{ background: i < score ? meta.color : 'var(--surface-3)' }} />
        ))}
      </div>
      <div className="strength-label">
        <span style={{ color: value ? meta.color : 'var(--ink-4)' }}>
          {value ? meta.label : 'Força da senha'}
        </span>
        <span className="hint">{value ? meta.hint : ''}</span>
      </div>
    </div>
  );
}

const isEmail = (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.trim());

/* ============================================================
   LOGIN
   ============================================================ */
function LoginPage() {
  const [email, setEmail] = useState('');
  const [pw, setPw] = useState('');
  const [remember, setRemember] = useState(true);
  const [err, setErr] = useState({});
  const [banner, setBanner] = useState('');
  const [loading, setLoading] = useState(false);

  const validate = () => {
    const e = {};
    if (!email.trim()) e.email = 'Informe seu e-mail.';
    else if (!isEmail(email)) e.email = 'E-mail inválido.';
    if (!pw) e.pw = 'Informe sua senha.';
    return e;
  };

  const submit = (ev) => {
    ev.preventDefault();
    setBanner('');
    const e = validate();
    setErr(e);
    if (Object.keys(e).length) return;
    setLoading(true);
    setTimeout(() => {
      // demo: any address containing "errado" simulates a failed login
      if (/errad/i.test(email)) {
        setLoading(false);
        setBanner('E-mail ou senha incorretos. Verifique e tente novamente.');
      } else {
        window.location.href = APP_URL;
      }
    }, 1100);
  };

  return (
    <div className="auth-page">
      <BrandPanel
        eyebrow="Finanças pessoais"
        head="Seu dinheiro, com a clareza de um cofre."
        lede="Saldo, faturas, metas e renda fixa — tudo em um só painel, sempre em tempo real."
        feats={[
          'Saldo líquido honesto, já descontando faturas em aberto',
          'Orçamentos, cofres de metas e carteira de renda fixa',
          'Importe extratos em CSV e OFX com deduplicação automática',
        ]} />

      <div className="form-panel">
        <div className="form-topbar">
          <span className="lbl">Novo por aqui?</span>
          <a className="ghost-link" href="Register.html">Criar conta</a>
        </div>

        <form className="auth-card" onSubmit={submit} noValidate>
          <div className="auth-eyebrow">Acesse sua conta</div>
          <h1 className="auth-title">Entrar</h1>
          <p className="auth-lede">Bem-vindo de volta. Entre para continuar.</p>

          <div className="auth-form">
            {banner && <AuthBanner>{banner}</AuthBanner>}

            <Field icon="mail" label="E-mail" name="email" type="email" inputMode="email"
              autoComplete="email" placeholder="voce@email.com"
              value={email} onChange={(v) => { setEmail(v); if (err.email) setErr({ ...err, email: null }); }}
              error={err.email} />

            <div>
              <PasswordField label="Senha" name="password" autoComplete="current-password"
                placeholder="••••••••"
                value={pw} onChange={(v) => { setPw(v); if (err.pw) setErr({ ...err, pw: null }); }}
                error={err.pw} />
            </div>

            <div className="auth-row">
              <Checkbox checked={remember} onChange={setRemember} name="remember">Lembrar de mim</Checkbox>
              <a className="link-quiet" href="#">Esqueceu a senha?</a>
            </div>

            <SubmitButton loading={loading} loadingLabel="Entrando…">Entrar</SubmitButton>
          </div>

          <div className="auth-switch">
            Não tem uma conta? <a href="Register.html">Criar conta gratuita</a>
          </div>
          <p className="auth-legal">
            Protegido por autenticação JWT. Ao entrar você concorda com os
            {' '}<a href="#">Termos</a> e a <a href="#">Política de Privacidade</a>.
          </p>
        </form>
      </div>
    </div>
  );
}

/* ============================================================
   REGISTER
   ============================================================ */
function RegisterPage() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [pw, setPw] = useState('');
  const [terms, setTerms] = useState(false);
  const [err, setErr] = useState({});
  const [loading, setLoading] = useState(false);

  const validate = () => {
    const e = {};
    if (!email.trim()) e.email = 'Informe seu e-mail.';
    else if (!isEmail(email)) e.email = 'E-mail inválido.';
    if (!pw) e.pw = 'Crie uma senha.';
    else if (pw.length < 8) e.pw = 'A senha precisa ter ao menos 8 caracteres.';
    if (!terms) e.terms = true;
    return e;
  };

  const submit = (ev) => {
    ev.preventDefault();
    const e = validate();
    setErr(e);
    if (Object.keys(e).length) return;
    setLoading(true);
    setTimeout(() => { window.location.href = APP_URL; }, 1200);
  };

  return (
    <div className="auth-page">
      <BrandPanel
        eyebrow="Comece grátis"
        head="Organize toda a sua vida financeira hoje."
        lede="Crie sua conta em menos de um minuto. Cada usuário vê apenas os próprios dados — sempre isolados."
        feats={[
          'Conecte contas, cartões e investimentos em um só lugar',
          'Acompanhe orçamentos e metas mês a mês',
          'Apenas R$ — entrada manual ou importação de extratos',
        ]} />

      <div className="form-panel">
        <div className="form-topbar">
          <span className="lbl">Já tem conta?</span>
          <a className="ghost-link" href="Login.html">Entrar</a>
        </div>

        <form className="auth-card" onSubmit={submit} noValidate>
          <div className="auth-eyebrow">Criar conta</div>
          <h1 className="auth-title">Crie sua conta</h1>
          <p className="auth-lede">Leva menos de um minuto. Sem cartão de crédito.</p>

          <div className="auth-form">
            <Field icon="user" label="Nome" name="name" autoComplete="name"
              placeholder="Como podemos te chamar?"
              value={name} onChange={setName} error={err.name} />

            <Field icon="mail" label="E-mail" name="email" type="email" inputMode="email"
              autoComplete="email" placeholder="voce@email.com"
              value={email} onChange={(v) => { setEmail(v); if (err.email) setErr({ ...err, email: null }); }}
              error={err.email} />

            <div>
              <PasswordField label="Senha" name="password" autoComplete="new-password"
                placeholder="Mínimo de 8 caracteres"
                value={pw} onChange={(v) => { setPw(v); if (err.pw) setErr({ ...err, pw: null }); }}
                error={err.pw} />
              <StrengthMeter value={pw} />
            </div>

            <div style={{ marginTop: 2 }}>
              <Checkbox checked={terms} onChange={(v) => { setTerms(v); if (err.terms) setErr({ ...err, terms: null }); }} name="terms">
                Li e aceito os <a className="link-quiet" href="#">Termos</a> e a <a className="link-quiet" href="#">Política de Privacidade</a>
              </Checkbox>
              {err.terms && <div className="auth-err" style={{ marginTop: 7 }}><Icon name="alertCircle" size={13} /> É preciso aceitar para continuar.</div>}
            </div>

            <SubmitButton loading={loading} loadingLabel="Criando conta…">Criar conta</SubmitButton>
          </div>

          <div className="auth-switch">
            Já tem uma conta? <a href="Login.html">Entrar</a>
          </div>
        </form>
      </div>
    </div>
  );
}

/* ============================================================
   ERROR PAGES (404 / 500 / 403)
   ============================================================ */
const ERRORS = {
  '404': {
    code: '404', icon: 'compass', eyebrow: 'Página não encontrada',
    title: 'Esta página saiu do cofre.',
    msg: 'O endereço que você procurou não existe ou foi movido. Verifique o link ou volte para um lugar conhecido.',
    primary: { label: 'Ir para Visão Geral', icon: 'grid', href: APP_URL },
  },
  '500': {
    code: '500', icon: 'alertTriangle', eyebrow: 'Erro do servidor',
    title: 'Algo deu errado do nosso lado.',
    msg: 'Encontramos um problema inesperado ao processar sua solicitação. Seus dados estão seguros — tente novamente em instantes.',
    primary: { label: 'Tentar novamente', icon: 'repeat', href: APP_URL },
  },
  '403': {
    code: '403', icon: 'lockKey', eyebrow: 'Acesso restrito',
    title: 'Você não tem acesso a isto.',
    msg: 'Este recurso pertence a outra conta. Por isolamento de dados, cada usuário vê apenas os próprios registros.',
    primary: { label: 'Voltar ao painel', icon: 'grid', href: APP_URL },
  },
};

function ErrorPage({ code = '404' }) {
  const e = ERRORS[code] || ERRORS['404'];
  const ref = 'MV-' + code + '-' + Math.random().toString(36).slice(2, 8).toUpperCase();
  return (
    <div className="error-page">
      <div className="error-watermark" />
      <div className="error-watermark inner" />

      <div className="error-card fade-in">
        <div className="error-brand"><LogoMark size={24} /></div>

        <div className="error-glyph" style={{ color: 'var(--frost-deep)' }}>
          <Icon name={e.icon} size={28} />
        </div>

        <div className="error-code">
          {e.code.split('').map((d, i) => (
            <span key={i} className={i === 1 ? 'accent' : ''}>{d}</span>
          ))}
        </div>

        <div className="error-eyebrow">{e.eyebrow}</div>
        <h1 className="error-title">{e.title}</h1>
        <p className="error-msg">{e.msg}</p>

        <div className="error-actions">
          <a className="btn-primary" href={e.primary.href}>
            <Icon name={e.primary.icon} size={16} /> {e.primary.label}
          </a>
          <a className="btn-ghost" href={APP_URL}>
            <Icon name="arrowLeft" size={16} /> Voltar
          </a>
        </div>

        <div className="error-ref">
          Código {e.code} <span className="dot">·</span> ref {ref}
        </div>
      </div>
    </div>
  );
}

function mountAuth(node) {
  ReactDOM.createRoot(document.getElementById('root')).render(node);
}

Object.assign(window, {
  LoginPage, RegisterPage, ErrorPage, mountAuth,
  LogoLight, BrandPanel, Field, PasswordField, Checkbox,
});
