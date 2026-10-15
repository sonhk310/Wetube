import { useDispatch, useSelector } from 'react-redux';
import type { TypedUseSelectorHook } from 'react-redux';
import type { RootState, AppDispatch } from '../store/store';

// Chuẩn hóa useDispatch với AppDispatch
export const useAppDispatch: () => AppDispatch = useDispatch;

// Chuẩn hóa useSelector với RootState
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;