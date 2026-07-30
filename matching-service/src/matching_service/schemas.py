"""Pydantic request/response models for the HTTP boundary of ``/match``.

Kept separate from rules.py's dataclasses on purpose: these describe the wire
format the orchestrator sends and receives, while rules.py describes the
domain the matching logic actually reasons about. A field renamed on one side
should not silently rename itself on the other.
"""

from datetime import datetime

from pydantic import BaseModel, Field


class LoadIn(BaseModel):
    id: str
    origin_city: str
    cargo_type: str
    weight_kg: float = Field(gt=0)
    pickup_window_start: datetime
    pickup_window_end: datetime


class TruckIn(BaseModel):
    id: str
    current_city: str
    vehicle_type: str
    capacity_kg: float = Field(gt=0)
    available_from: datetime
    available_until: datetime


class MatchRequest(BaseModel):
    load: LoadIn
    trucks: list[TruckIn]


class MatchResultOut(BaseModel):
    truck_id: str
    score: float
    reasons: list[str]


class MatchResponse(BaseModel):
    matches: list[MatchResultOut]
