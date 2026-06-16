const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
  }
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
	const response = await fetch(`${API_BASE_URL}${path}`, {
		headers: {
			'Content-Type': 'application/json',
      ...init.headers,
    },
    ...init,
	});

	if (!response.ok) {
		let message = `Request failed with ${response.status}`;
		try {
			const body = (await response.json()) as { message?: string };
			message = body.message ?? message;
		} catch {
			// Keep the HTTP fallback when the server returns no JSON body.
		}
		throw new ApiError(message, response.status);
	}

	return response.json() as Promise<T>;
}
