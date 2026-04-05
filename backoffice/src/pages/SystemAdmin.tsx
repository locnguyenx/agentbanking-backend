import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import api from '../api/client'
import { HealthCardGrid } from '../components/HealthCard'
import { MetricsPanel } from '../components/MetricsPanel'
import { AuditLogTable } from '../components/AuditLogTable'

export function SystemAdmin() {
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

  const { data: auditData, isLoading: auditLoading, refetch: refetchAudit } = useQuery({
    queryKey: ['admin-audit-logs'],
    queryFn: () => api.getAdminAuditLogs({ size: 50 }),
  })

  const services = healthData?.services || []

  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold mb-6">System Administration</h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        <div className="lg:col-span-2">
          <HealthCardGrid 
            services={services}
            summary={healthData?.summary}
            isLoading={healthLoading}
            onRefresh={refetchHealth}
            onSelectService={setSelectedService}
            selectedService={selectedService}
          />
        </div>
        <div>
          <MetricsPanel
            service={selectedService}
            metrics={metricsData}
            isLoading={metricsLoading}
            services={services}
            onSelectService={setSelectedService}
          />
        </div>
      </div>

      <div className="mb-4 flex justify-between items-center">
        <h2 className="text-lg font-semibold">Audit Logs</h2>
        <button
          onClick={() => refetchAudit()}
          className="px-4 py-2 bg-teal-600 text-white rounded hover:bg-teal-700 transition-colors"
        >
          Refresh
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
      />
    </div>
  )
}
