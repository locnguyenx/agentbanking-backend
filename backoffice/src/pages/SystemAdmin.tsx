import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import api from '../api/client'
import { HealthCardGrid } from '../components/HealthCard'
import { MetricsPanel } from '../components/MetricsPanel'
import { AuditLogTable } from '../components/AuditLogTable'

type TabType = 'health' | 'metrics' | 'audit'

export function SystemAdmin() {
  const [activeTab, setActiveTab] = useState<TabType>('health')
  const [selectedService, setSelectedService] = useState<string | null>(null)

  const { data: healthData, isLoading: healthLoading, refetch: refetchHealth } = useQuery({
    queryKey: ['admin-health'],
    queryFn: () => api.getAdminHealthAll(),
  })

  const { data: metricsData, isLoading: metricsLoading } = useQuery({
    queryKey: ['admin-metrics', selectedService],
    queryFn: () => selectedService 
      ? api.getAdminMetrics(selectedService)
      : Promise.resolve(null),
    enabled: !!selectedService,
  })

  const [auditFilters, setAuditFilters] = useState({
    service: 'auth',
    page: 0,
    size: 50,
  })

  const { data: auditData, isLoading: auditLoading, refetch: refetchAudit } = useQuery({
    queryKey: ['admin-audit-logs', auditFilters],
    queryFn: () => api.getAdminAuditLogs({ 
      service: auditFilters.service,
      page: auditFilters.page,
      size: auditFilters.size,
    }),
  })

  const services = healthData?.services || []

  const tabs = [
    { id: 'health' as TabType, label: 'Health Dashboard', icon: '🟢' },
    { id: 'metrics' as TabType, label: 'Metrics Panel', icon: '📊' },
    { id: 'audit' as TabType, label: 'Audit Logs', icon: '📋' },
  ]

  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold mb-6">System Administration</h1>

      {/* Tab Navigation */}
      <div className="mb-6">
        <div className="border-b border-gray-200">
          <nav className="-mb-px flex space-x-8">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`
                  whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors
                  ${activeTab === tab.id
                    ? 'border-teal-500 text-teal-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }
                `}
              >
                <span className="mr-2">{tab.icon}</span>
                {tab.label}
                {tab.id === 'health' && healthData?.summary && (
                  <span className={`ml-2 px-2 py-0.5 text-xs rounded-full
                    ${healthData.summary.unhealthy > 0 
                      ? 'bg-red-100 text-red-800' 
                      : 'bg-green-100 text-green-800'
                    }`}>
                    {healthData.summary.healthy}/{healthData.summary.total}
                  </span>
                )}
              </button>
            ))}
          </nav>
        </div>
      </div>

      {/* Tab Content */}
      <div className="bg-white rounded-lg shadow">
        {activeTab === 'health' && (
          <div className="p-6">
            <div className="flex justify-between items-center mb-4">
              <div className="text-sm text-gray-500">
                Last updated: {healthData?.timestamp ? new Date(healthData.timestamp).toLocaleString() : 'Never'}
              </div>
              <button
                onClick={() => refetchHealth()}
                disabled={healthLoading}
                className="px-4 py-2 bg-teal-600 text-white rounded hover:bg-teal-700 transition-colors disabled:opacity-50"
              >
                {healthLoading ? 'Refreshing...' : 'Refresh'}
              </button>
            </div>
            <HealthCardGrid 
              services={services}
              summary={healthData?.summary}
              isLoading={healthLoading}
              onRefresh={refetchHealth}
              onSelectService={(service) => {
                setSelectedService(service)
                setActiveTab('metrics')
              }}
              selectedService={selectedService}
            />
          </div>
        )}

        {activeTab === 'metrics' && (
          <div className="p-6">
            <MetricsPanel
              service={selectedService}
              metrics={metricsData}
              isLoading={metricsLoading}
              services={services}
              onSelectService={setSelectedService}
            />
          </div>
        )}

        {activeTab === 'audit' && (
          <div className="p-6">
            <div className="flex flex-wrap gap-4 mb-4 items-center">
              <div className="flex items-center gap-2">
                <label className="text-sm text-gray-600">Service:</label>
                <select
                  value={auditFilters.service}
                  onChange={(e) => setAuditFilters({ ...auditFilters, service: e.target.value, page: 0 })}
                  className="border border-gray-300 rounded px-3 py-1.5 text-sm"
                >
                  <option value="auth">Auth Service</option>
                  <option value="onboarding">Onboarding Service</option>
                  <option value="ledger">Ledger Service</option>
                  <option value="rules">Rules Service</option>
                  <option value="biller">Biller Service</option>
                  <option value="switch">Switch Service</option>
                  <option value="orchestrator">Orchestrator Service</option>
                </select>
              </div>
              <button
                onClick={() => refetchAudit()}
                disabled={auditLoading}
                className="px-4 py-2 bg-teal-600 text-white rounded hover:bg-teal-700 transition-colors disabled:opacity-50"
              >
                {auditLoading ? 'Loading...' : 'Refresh'}
              </button>
            </div>
            <AuditLogTable
              logs={auditData?.content || []}
              isLoading={auditLoading}
              pagination={{
                page: auditData?.page || 0,
                size: auditData?.size || 50,
                totalElements: auditData?.totalElements || 0,
                totalPages: auditData?.totalPages || 0,
              }}
              onPageChange={(page) => setAuditFilters({ ...auditFilters, page })}
            />
          </div>
        )}
      </div>
    </div>
  )
}