package com.commafeed.frontend.resource;

import com.commafeed.backend.model.FeedEntryNote;
import com.commafeed.backend.model.User;
import com.commafeed.backend.service.FeedEntryNoteService;
import com.commafeed.frontend.model.FeedEntryNoteResponse;
import com.commafeed.frontend.model.request.AddNoteRequest;
import com.commafeed.security.AuthenticationContext;
import com.commafeed.security.Roles;
import com.google.common.base.Preconditions;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/rest/note")
@RolesAllowed(Roles.USER)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Singleton
@Tag(name = "Notes")
public class FeedEntryNoteREST {

    private final AuthenticationContext authenticationContext;
    private final FeedEntryNoteService feedEntryNoteService;

    @POST
    @Transactional
    @Operation(
            summary = "Create a note for a feed entry",
            description = "Create a note for a feed entry")
    public Response createNote(
            @Valid @Parameter(description = "Add Note Request", required = true)
                    AddNoteRequest req) {
        Preconditions.checkNotNull(req);

        User user = authenticationContext.getCurrentUser();
        feedEntryNoteService.addNote(user, req);

        return Response.ok().build();
    }

    @GET
    @Transactional
    @Operation(
            summary = "Get all notes for the user",
            description = "Get all notes belonging to the currently authenticated user")
    public Response getNotes() {
        User user = authenticationContext.getCurrentUser();
        List<FeedEntryNote> notes = feedEntryNoteService.getNotes(user);
        List<FeedEntryNoteResponse> models =
                notes.stream().map(FeedEntryNoteResponse::build).toList();
        return Response.ok(models).build();
    }

    @Path("/{id}")
    @GET
    @Transactional
    @Operation(
            summary = "Get a single note by id",
            description = "Get a single note by id belonging to the currently authenticated user")
    public Response getNote(
            @Parameter(description = "note id", required = true) @PathParam("id") Long id) {
        User user = authenticationContext.getCurrentUser();
        FeedEntryNote note = feedEntryNoteService.getNote(user, id);
        if (note == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(FeedEntryNoteResponse.build(note)).build();
    }
}
