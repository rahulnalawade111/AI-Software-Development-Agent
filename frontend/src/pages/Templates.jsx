const TEMPLATES = [
  { id: 'react-starter', name: 'React Starter', stack: ['React', 'Vite'], description: 'Single-page React app with routing and components scaffolded.' },
  { id: 'spring-boot-starter', name: 'Spring Boot Starter', stack: ['Spring Boot', 'Java'], description: 'REST API with Spring Boot 3, JPA and MySQL.' },
  { id: 'fullstack-react-springboot', name: 'Fullstack React + Spring Boot', stack: ['React', 'Spring Boot', 'MySQL'], description: 'Frontend + backend + database wiring with JWT auth starter.' },
  { id: 'node-express-starter', name: 'Node.js + Express', stack: ['Node.js'], description: 'Express REST API with routing and middleware.' },
  { id: 'python-flask-starter', name: 'Python + Flask', stack: ['Python'], description: 'Flask app with blueprints and templates.' }
];

export default function Templates() {
  return (
    <div className="page">
      <div className="page-head"><h1>Templates</h1></div>
      <div className="template-grid">
        {TEMPLATES.map(t => (
          <div className="card template-card" key={t.id}>
            <h3>{t.name}</h3>
            <p>{t.description}</p>
            <div className="project-meta">{t.stack.map(s => <span key={s} className="badge gray">{s}</span>)}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
