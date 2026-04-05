import React from 'react'

interface JvmMetrics {
  memoryUsedMb: number;
  memoryMaxMb: number;
  threadsActive: number;
  cpuUsagePercent: number;
  uptimeSeconds: number;
}

interface HttpMetrics {
  requestsTotal: number;
  errorsTotal: number;
  avgResponseTimeMs: number;
}

interface MetricsData {
  serviceName: string;
  jvm: JvmMetrics;
  http: HttpMetrics;
  timestamp: string;
}

interface MetricsPanelProps {
  service: string | null;
  metrics: MetricsData | null | undefined;
  isLoading?: boolean;
  services?: { name: string }[];
  onSelectService?: (name: string | null) => void;
}

export const MetricsPanel: React.FC<MetricsPanelProps> = ({
  service,
  metrics,
  isLoading,
  services = [],
  onSelectService,
}) => {
  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <h2 style={{ margin: 0 }}>Service Metrics</h2>
      </div>

      <div style={{ marginBottom: '16px' }}>
        <label style={{ display: 'block', fontSize: '14px', marginBottom: '6px', color: '#64748b' }}>
          Select Service
        </label>
        <select
          value={service || ''}
          onChange={(e) => onSelectService?.(e.target.value || null)}
          style={{
            width: '100%',
            padding: '8px 12px',
            borderRadius: '6px',
            border: '1px solid #e2e8f0',
            fontSize: '14px',
          }}
        >
          <option value="">Select a service...</option>
          {services.map((s) => (
            <option key={s.name} value={s.name}>
              {s.name}
            </option>
          ))}
        </select>
      </div>

      {!service && (
        <div style={{ textAlign: 'center', padding: '40px', color: '#64748b' }}>
          Select a service to view metrics
        </div>
      )}

      {service && isLoading && (
        <div style={{ textAlign: 'center', padding: '40px' }}>Loading metrics...</div>
      )}

      {service && !isLoading && metrics && 'error' in metrics && (
        <div style={{ textAlign: 'center', padding: '40px', color: '#ef4444' }}>
          {String(metrics.error) || 'Metrics unavailable'}
        </div>
      )}

      {service && !isLoading && metrics && !('error' in metrics) && (
        <div>
          <h3 style={{ margin: '0 0 12px', fontSize: '14px', color: '#64748b' }}>
            {metrics.serviceName}
          </h3>

          <div style={{ marginBottom: '16px' }}>
            <h4 style={{ fontSize: '12px', color: '#94a3b8', margin: '0 0 8px', textTransform: 'uppercase' }}>
              JVM
            </h4>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>Memory Used</div>
                <div style={{ fontSize: '18px', fontWeight: 600 }}>
                  {metrics.jvm.memoryUsedMb.toFixed(1)} MB
                </div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>Memory Max</div>
                <div style={{ fontSize: '18px', fontWeight: 600 }}>
                  {metrics.jvm.memoryMaxMb.toFixed(1)} MB
                </div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>Threads</div>
                <div style={{ fontSize: '18px', fontWeight: 600 }}>
                  {metrics.jvm.threadsActive}
                </div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>CPU</div>
                <div style={{ fontSize: '18px', fontWeight: 600 }}>
                  {metrics.jvm.cpuUsagePercent.toFixed(1)}%
                </div>
              </div>
            </div>
          </div>

          <div>
            <h4 style={{ fontSize: '12px', color: '#94a3b8', margin: '0 0 8px', textTransform: 'uppercase' }}>
              HTTP
            </h4>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>Requests</div>
                <div style={{ fontSize: '18px', fontWeight: 600 }}>
                  {metrics.http.requestsTotal.toLocaleString()}
                </div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>Errors</div>
                <div style={{ fontSize: '18px', fontWeight: 600, color: metrics.http.errorsTotal > 0 ? '#ef4444' : 'inherit' }}>
                  {metrics.http.errorsTotal}
                </div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>Avg Response</div>
                <div style={{ fontSize: '18px', fontWeight: 600 }}>
                  {metrics.http.avgResponseTimeMs.toFixed(1)} ms
                </div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#64748b' }}>Uptime</div>
                <div style={{ fontSize: '18px', fontWeight: 600 }}>
                  {Math.floor(metrics.jvm.uptimeSeconds / 3600)}h {Math.floor((metrics.jvm.uptimeSeconds % 3600) / 60)}m
                </div>
              </div>
            </div>
          </div>

          <p style={{ fontSize: '11px', color: '#94a3b8', margin: '16px 0 0', textAlign: 'right' }}>
            Updated: {new Date(metrics.timestamp).toLocaleTimeString()}
          </p>
        </div>
      )}
    </div>
  )
}
