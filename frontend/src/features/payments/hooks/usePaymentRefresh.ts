import { useQueryClient } from '@tanstack/react-query'

export function usePaymentRefresh(workOrderId: string) {
  const queryClient = useQueryClient()

  return () => {
    queryClient.invalidateQueries({ queryKey: ['work-order-payment', workOrderId] })
    queryClient.invalidateQueries({ queryKey: ['payments'] })
    queryClient.invalidateQueries({ queryKey: ['audit'] })
    queryClient.invalidateQueries({ queryKey: ['work-order', workOrderId] })
    queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    queryClient.invalidateQueries({ queryKey: ['work-order-history'] })
    queryClient.invalidateQueries({ queryKey: ['work-order-timeline', workOrderId] })
  }
}
