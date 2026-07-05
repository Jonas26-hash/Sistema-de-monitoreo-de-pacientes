import { useState, useCallback } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Modal, App } from 'antd';
import { ExclamationCircleOutlined } from '@ant-design/icons';
import api from '../services/api';
import { showCrudSuccess } from '../utils/notifications';
import dayjs from 'dayjs';

interface CrudConfig<T> {
  key: string;
  endpoint: string;
  queryParams?: Record<string, unknown>;
  createEndpoint?: string;
  canUpdate?: boolean;
  canDelete?: boolean;
  dateFields?: (keyof T)[];
  dateTimeFields?: (keyof T)[];
}

export function normalizeResponse<T>(payload: T[] | { content?: T[]; totalElements?: number; totalPages?: number }) {
  if (Array.isArray(payload)) {
    return {
      content: payload,
      totalElements: payload.length,
      totalPages: 1,
    };
  }
  return {
    content: payload?.content ?? [],
    totalElements: payload?.totalElements ?? payload?.content?.length ?? 0,
    totalPages: payload?.totalPages ?? 1,
  };
}

export function useCrud<T extends { id?: number }>({
  key,
  endpoint,
  queryParams,
  createEndpoint,
  canUpdate = true,
  canDelete = true,
  dateFields = [],
  dateTimeFields = [],
}: CrudConfig<T>) {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<T | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  const queryKey = [key, page, search, queryParams];

  const listQuery = useQuery({
    queryKey,
    queryFn: async () => {
      const params: Record<string, unknown> = { page, size: 10, ...queryParams };
      if (search) params.search = search;
      const res = await api.get(endpoint, { params });
      return normalizeResponse<T>(res.data);
    },
  });

  const createMutation = useMutation({
    mutationFn: (data: T) => api.post(createEndpoint ?? endpoint, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [key] });
      showCrudSuccess('creado');
      setModalOpen(false);
    },
    onError: (err: unknown) => {
      const d = (err as { response?: { data?: { error?: string; mensaje?: string } } }).response?.data;
      Modal.error({ title: 'Error', content: d?.mensaje || d?.error || 'Error al crear', centered: true });
    },
  });

  const updateMutation = useMutation({
    mutationFn: (data: T) => {
      if (!canUpdate) return Promise.reject(new Error('Este recurso no permite actualización desde el backend'));
      return api.put(`${endpoint}/${data.id}`, data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [key] });
      showCrudSuccess('actualizado');
      setModalOpen(false);
    },
    onError: (err: unknown) => {
      const d = (err as { response?: { data?: { error?: string; mensaje?: string } } }).response?.data;
      Modal.error({ title: 'Error', content: d?.mensaje || d?.error || 'Error al actualizar', centered: true });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => {
      if (!canDelete) return Promise.reject(new Error('Este recurso no permite eliminación desde el backend'));
      return api.delete(`${endpoint}/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [key] });
      showCrudSuccess('eliminado');
    },
    onError: (err: unknown) => {
      const d = (err as { response?: { data?: { error?: string; mensaje?: string } } }).response?.data;
      Modal.error({ title: 'Error', content: d?.mensaje || d?.error || 'Error al eliminar', centered: true });
    },
  });

  const openCreate = useCallback(() => {
    setEditing(null);
    setModalOpen(true);
  }, []);

  const openEdit = useCallback((record: T) => {
    const converted = { ...record };
    dateFields.forEach((field) => {
      const val = converted[field];
      if (typeof val === 'string') {
        (converted as any)[field] = dayjs(val as string);
      }
    });
    dateTimeFields.forEach((field) => {
      const val = converted[field];
      if (typeof val === 'string') {
        (converted as any)[field] = dayjs(val as string);
      }
    });
    setEditing(converted);
    setModalOpen(true);
  }, [dateFields, dateTimeFields]);

  const closeModal = useCallback(() => {
    setModalOpen(false);
    setEditing(null);
  }, []);

  const handleSave = useCallback(async (values: T) => {
    const prepared = { ...values };
    dateFields.forEach((field) => {
      const val = (prepared as any)[field];
      if (val && typeof val.format === 'function') {
        (prepared as any)[field] = val.format('YYYY-MM-DD');
      }
    });
    dateTimeFields.forEach((field) => {
      const val = (prepared as any)[field];
      if (val && typeof val.format === 'function') {
        (prepared as any)[field] = val.format('YYYY-MM-DDTHH:mm:ss');
      }
    });
    if (editing?.id) {
      updateMutation.mutate({ ...prepared, id: editing.id });
    } else {
      createMutation.mutate(prepared);
    }
  }, [editing, createMutation, updateMutation, dateFields, dateTimeFields]);

  const handleDelete = useCallback((id: number) => {
    Modal.confirm({
      title: '¿Eliminar este registro?',
      icon: <ExclamationCircleOutlined />,
      content: 'Esta acción no se puede deshacer',
      okText: 'Eliminar',
      okType: 'danger',
      cancelText: 'Cancelar',
      centered: true,
      onOk: () => deleteMutation.mutate(id),
    });
  }, [deleteMutation]);

  const isError = listQuery.isError;
  const error = listQuery.error;

  return {
    listQuery,
    createMutation,
    updateMutation,
    deleteMutation,
    search,
    setSearch,
    page,
    setPage,
    editing,
    modalOpen,
    openCreate,
    openEdit,
    closeModal,
    handleSave,
    handleDelete,
    canUpdate,
    canDelete,
    loading: listQuery.isLoading,
    data: listQuery.data as { content?: T[]; totalElements?: number; totalPages?: number } | undefined,
    isError,
    error,
  };
}
