/*
 * Copyright (c) 2010, IETR/INSA of Rennes
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the IETR/INSA of Rennes nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
#ifndef SCHEDULER_H
#define SCHEDULER_H

#include "fifo.h"
#include "thread.h"
#include "dataflow.h"

#define MAX_ACTORS 1024

struct scheduler_s {
	int id; /** Unique ID of this scheduler */
	int schedulers_nb;

	/* Round robin */
	int num_actors; /** number of actors managed by this scheduler */
	struct actor_s **actors; /** static list of actors managed by this scheduler */
	int rr_next_schedulable; /** index of the next actor to schedule in last list */

	/* Data demand/driven scheduler */
	struct actor_s *schedulable[MAX_ACTORS]; /** dynamic list of the next actors to schedule */
	unsigned int ddd_next_entry; /** index of the next actor to schedule in last list */
	unsigned int ddd_next_schedulable; /** index of next actor added in the list */

	/* Multicore with data demand/driven scheduler */
	int round_robin; /** set to 1 when last scheduled actor is a result of round robin scheduling */
	/* ring topology */
	struct waiting_s *ring_waiting_schedulable; /** receiving list of some actors to schedule */
	struct waiting_s *ring_sending_schedulable; /** sending list of some actors to schedule */
	/* mesh topology */
	struct waiting_s **mesh_waiting_schedulable; /** receiving lists from other schedulers of some actors to schedule */

	/* Genetic algorithm */
	struct sync_s *sync;
	semaphore_struct sem_thread;
};

struct waiting_s {
	struct actor_s *waiting_actors[MAX_ACTORS];
	volatile unsigned int next_entry;
	unsigned int next_waiting;
};

/**
 * Initialize the given scheduler.
 */
void sched_init(struct scheduler_s *sched, int id, int num_actors,
		struct actor_s **actors, struct waiting_s *ring_waiting_schedulable,
		struct waiting_s *ring_sending_schedulable, int schedulers_nb,
		struct sync_s *sync);

/**
 * Initialize the actors mapped to the given scheduler.
 */
void sched_init_actors(struct scheduler_s *sched, struct schedinfo_s *si);

/**
 * Reinitialize the given scheduler.
 */
void sched_reinit(struct scheduler_s *sched, int num_actors,
		struct actor_s **actors, int use_ring_topology, int schedulers_nb);

/**
 * Returns the next actor in actors list.
 * This method is used by the round-robin scheduler.
 */
struct actor_s *sched_get_next(struct scheduler_s *sched);

/**
 * Add the actor to the schedulable or waiting list.
 * The list is chosen according to associate scheduler of the actor.
 */
void sched_add_schedulable(struct scheduler_s *sched,
		struct actor_s *actor, int use_ring_topology);

/**
 * Add waited actors to the schedulable or waiting list.
 * The list is chosen according to associate scheduler of the actor.
 * This function use ring topology of communications.
 */
void sched_add_ring_waiting_list(struct scheduler_s *sched);

/**
 * Add waited actors to the schedulable list.
 * This function use mesh topology of communications.
 */
void sched_add_mesh_waiting_list(struct scheduler_s *sched);

/**
 * Returns the next schedulable actor, or NULL if no actor is schedulable.
 * The actor is removed from the schedulable list.
 * This method is used by the data/demand driven scheduler.
 */
struct actor_s *sched_get_next_schedulable(struct scheduler_s *sched,
		int use_ring_topology);

#endif
