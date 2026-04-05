interface ServiceInfo {
  name: string;
  port: number;
  purpose: string;
  status: 'UP' | 'DOWN' | 'DEGRADED';
  lastChecked: string;
  error?: string;
}

interface HealthSummary {
  total: number;
  healthy: number;
  unhealthy: number;
}

interface HealthCardProps {
  name: string;
  port: number;
  purpose: string;
  status: 'UP' | 'DOWN' | 'DEGRADED';
  lastChecked: string;
  error?: string;
  onDrillDown?: (name: string) => void;
  selected?: boolean;
}

const statusClass: Record<string, string> = {
  UP: 'badge-success',
  DOWN: 'badge-error',
  DEGRADED: 'badge-warning',
}

export const HealthCard: React.FC<HealthCardProps> = ({
  name,
  port,
  purpose,
  status,
  lastChecked,
  error,
  onDrillDown,
  selected,
}) => (
  <div
    data-testid="health-card"
    className={`card ${selected ? 'ring-2 ring-teal-500' : ''}`}
    style={{ cursor: onDrillDown ? 'pointer' : 'default' }}
    onClick={() => onDrillDown?.(name)}
  >
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <h3 style={{ margin: 0 }}>{name}</h3>
      <span className={`badge ${statusClass[status] || 'badge-info'}`}>{status}</span>
    </div>
    <p style={{ color: '#64748b', margin: '8px 0 0' }}>Port: {port} &middot; {purpose}</p>
    {error && (
      <p style={{ color: '#ef4444', fontSize: '12px', margin: '4px 0 0' }}>{error}</p>
    )}
    <p style={{ color: '#94a3b8', fontSize: '12px', margin: '4px 0 0' }}>
      Last checked: {new Date(lastChecked).toLocaleTimeString()}
    </p>
  </div>
)

interface HealthCardGridProps {
  services: ServiceInfo[];
  summary?: HealthSummary;
  isLoading?: boolean;
  onRefresh?: () => void;
  onSelectService?: (name: string | null) => void;
  selectedService?: string | null;
}

export const HealthCardGrid: React.FC<HealthCardGridProps> = ({
  services,
  summary,
  isLoading,
  onRefresh,
  onSelectService,
  selectedService,
}) => {
  if (isLoading) {
    return (
      <div className="card">
        <div style={{ textAlign: 'center', padding: '40px' }}>Loading...</div>
      </div>
    )
  }

  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <h2 style={{ margin: 0 }}>Service Health</h2>
        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          {summary && (
            <div style={{ display: 'flex', gap: '12px', fontSize: '14px' }}>
              <span style={{ color: '#22c55e' }}>{summary.healthy} healthy</span>
              <span style={{ color: summary.unhealthy > 0 ? '#ef4444' : '#64748b' }}>
                {summary.unhealthy} down
              </span>
            </div>
          )}
          {onRefresh && (
            <button onClick={onRefresh} className="btn btn-secondary" style={{ padding: '6px 12px' }}>
              Refresh
            </button>
          )}
        </div>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '12px' }}>
        {services.map((service) => (
          <HealthCard
            key={service.name}
            name={service.name}
            port={service.port}
            purpose={service.purpose}
            status={service.status}
            lastChecked={service.lastChecked}
            error={service.error}
            onDrillDown={onSelectService}
            selected={selectedService === service.name}
          />
        ))}
      </div>
    </div>
  )
}
