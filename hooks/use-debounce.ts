import { useEffect, useState } from "react";

// Хук для реализации debounce
export default function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value); // Устанавливаем значение после задержки
    }, delay);

    return () => {
      clearTimeout(handler); // Очищаем таймер при размонтировании или изменении значения
    };
  }, [value, delay]);

  return debouncedValue;
}