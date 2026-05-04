import { useRouter } from "vue-router";
import { clearToken } from "../state/auth";

export function useAuthFailureHandler() {
  const router = useRouter();
  return async function handleAuthFailure() {
    clearToken();
    await router.replace("/login");
  };
}

