(*****************************************************************************)
(* ORCC frontend                                                             *)
(* Copyright (c) 2008-2009, IETR/INSA of Rennes.                             *)
(* All rights reserved.                                                      *)
(*                                                                           *)
(* This software is governed by the CeCILL-B license under French law and    *)
(* abiding by the rules of distribution of free software. You can  use,      *)
(* modify and/ or redistribute the software under the terms of the CeCILL-B  *)
(* license as circulated by CEA, CNRS and INRIA at the following URL         *)
(* "http://www.cecill.info".                                                 *)
(*                                                                           *)
(* Matthieu WIPLIEZ <Matthieu.Wipliez@insa-rennes.fr                         *)
(*****************************************************************************)

open Printf
open Options

(*****************************************************************************)
(*****************************************************************************)
(*****************************************************************************)
(* options and main *)

(** main function *)
let _ =
  let options = init_options () in

	(* if debug enabled, records backtraces *)
	Printexc.record_backtrace options.o_debug;

	Codegen_xdf.start options;
	if options.o_profiling then (
		printf "parsing CAL = %f
converting = %f
transforming to SSA = %f
constant propagation, phase 1 = %f
constant propagation, phase 2 = %f
optimizations = %f
generating code = %f\n"
			!Cal_parser_wrapper.time
			!Ast2ir.time
			!Cal_ssa.time
			!Constprop.time_1
			!Constprop.time_2
			!Optims.time
			!Codegen_ir.time
	);
	
	if !Codegen_xdf.model_has_errors then (
		prerr_endline "Errors detected, compilation aborted.";
		exit 1
	);

	print_endline "ORCC frontend done.\n"
