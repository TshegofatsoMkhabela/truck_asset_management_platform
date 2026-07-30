# Slim, not the full python image: nothing in this service's dependencies (FastAPI,
# uvicorn) needs a C compiler or the extra tooling the full image carries, so slim is
# strictly smaller with nothing given up.
FROM python:3.11-slim
WORKDIR /app

# Dependencies installed from pyproject.toml before the source is copied, so an
# application-code edit doesn't invalidate Docker's dependency-install layer cache.
# -e (editable) is load-bearing here, not a style choice: a plain `pip install .`
# copies whatever is in src/ into site-packages at that instant, so with only the
# stub package present the installed copy would forever be that empty stub, and
# the later COPY of real source would update the working directory without
# updating what Python actually imports. Verified by hitting exactly that failure
# first: "Could not import module matching_service.main". An editable install
# instead points site-packages at ./src by reference, so the later COPY is what
# ends up being served.
COPY pyproject.toml .
RUN mkdir -p src/matching_service && touch src/matching_service/__init__.py
RUN pip install --no-cache-dir -e .

COPY src ./src

# Runs as a non-root user, matching the same reasoning as backend/Dockerfile: no
# process here needs root, so none is granted.
RUN useradd --system --create-home tamp
USER tamp

# No ENV defaults for PORT or any upstream URL: this service reads nothing from the
# environment today (it has no outbound calls and no config file), so there is
# nothing here for Compose to override.
EXPOSE 8000
CMD ["uvicorn", "matching_service.main:app", "--host", "0.0.0.0", "--port", "8000"]
