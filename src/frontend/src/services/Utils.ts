class Utils {
    static toHumanReadable(timestamp: number): string {
        const localDate = new Date(timestamp);
        return localDate.toLocaleString();
    }

    static createRequestInit(body: any, method: string): RequestInit {
        return {
            method: method.toLocaleUpperCase(),
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(body),
        };
    }
}

export { Utils };
