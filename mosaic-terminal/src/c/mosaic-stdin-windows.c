#include "mosaic.h"

#if defined(WIN32)

#include "cutils.h"
#include <Windows.h>

typedef struct stdinReaderImpl {
	HANDLE handles[2];
	BOOL closeStdin;
} stdinReaderImpl;

stdinReaderResult stdinReader_init(const char *path) {
	stdinReaderResult result = {};

	stdinReaderImpl *reader = calloc(1, sizeof(stdinReaderImpl));
	if (unlikely(reader == NULL)) {
		// result.reader is set to 0 which will trigger OOM.
		goto ret;
	}

	HANDLE stdin;
	if (path) {
		stdin = CreateFileA(path, GENERIC_READ, 0, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
		reader->closeStdin = TRUE;
	} else {
		stdin = GetStdHandle(STD_INPUT_HANDLE);
	}
	if (unlikely(stdin == INVALID_HANDLE_VALUE)) {
		result.error = GetLastError();
		goto err;
	}
	reader->handles[0] = stdin;

	HANDLE interruptEvent = CreateEvent(NULL, FALSE, FALSE, TEXT("TODO UUID"));
	if (unlikely(interruptEvent == NULL)) {
		result.error = GetLastError();
		goto err;
	}
	reader->handles[1] = interruptEvent;

	ret:
	return result;

	err:
	free(reader);
	goto ret;
}

stdinRead stdinReader_read(
	stdinReader *reader,
	void *buffer,
	int count
) {
	return stdinReader_readWithTimeout(reader, buffer, count, INFINITE);
}

stdinRead stdinReader_readWithTimeout(
	stdinReader *reader,
	void *buffer,
	int count,
	int timeoutMillis
) {
	stdinRead result = {};
	DWORD waitResult = WaitForMultipleObjects(2, reader->handles, FALSE, timeoutMillis);
	if (likely(waitResult == WAIT_OBJECT_0)) {
		LPDWORD read = 0;
		if (likely(ReadConsole(reader->handles[0], buffer, count, read, NULL) != 0)) {
			// TODO EOF?
			result.count = (*read);
		} else {
			goto err;
		}
	} else if (unlikely(waitResult == WAIT_FAILED)) {
		goto err;
	}
	// Else if the interrupt event was selected or we timed out, return a count of 0.

	ret:
	return result;

	err:
	result.error = GetLastError();
	goto ret;
}

platformError stdinReader_interrupt(stdinReader *reader) {
	return likely(SetEvent(reader->handles[1]) != 0)
		? 0
		: GetLastError();
}

platformError stdinReader_free(stdinReader *reader) {
	DWORD result = 0;
	if (unlikely(CloseHandle(reader->handles[1]) != 0)) {
		result = GetLastError();
	}
	if (unlikely(reader->closeStdin && CloseHandle(reader->handles[0]) && result != 0)) {
		result = GetLastError();
	}
	free(reader);
	return result;
}

#endif
