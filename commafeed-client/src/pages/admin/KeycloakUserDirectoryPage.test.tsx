import { i18n } from "@lingui/core"
import { I18nProvider } from "@lingui/react"
import { MantineProvider } from "@mantine/core"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import type React from "react"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { client } from "@/app/client"
import { KeycloakUserDirectoryPage } from "./KeycloakUserDirectoryPage"

vi.mock("@/app/client", () => ({
    client: {
        keycloakAdmin: {
            getUsers: vi.fn(),
            getUserDetails: vi.fn(),
        },
    },
    errorToStrings: vi.fn(() => []),
}))

describe("KeycloakUserDirectoryPage", () => {
    beforeEach(() => {
        vi.clearAllMocks()
        i18n.load("en", {})
        i18n.activate("en")
    })

    const renderWithProviders = (ui: React.ReactElement) =>
        render(
            <I18nProvider i18n={i18n}>
                <MantineProvider>{ui}</MantineProvider>
            </I18nProvider>
        )

    const mockUsers = [
        {
            keycloakId: "uuid-1111-2222",
            username: "john_keycloak",
            email: "john@example.com",
            firstName: "John",
            lastName: "Doe",
            enabled: true,
        },
    ]

    const mockDetail = {
        keycloakId: "uuid-1111-2222",
        username: "john_keycloak",
        email: "john@example.com",
        firstName: "John",
        lastName: "Doe",
        enabled: true,
        roles: ["admin-realm-role", "user-role"],
        groups: ["Engineering-Group"],
    }

    it("renders list without detail-only role or group fields and confirms absence of sorting controls", async () => {
        vi.mocked(client.keycloakAdmin.getUsers).mockResolvedValue({
            data: mockUsers,
        } as unknown as Awaited<ReturnType<typeof client.keycloakAdmin.getUsers>>)

        renderWithProviders(<KeycloakUserDirectoryPage />)

        await waitFor(() => {
            expect(screen.getByText("john_keycloak")).toBeInTheDocument()
        })

        // 1. Confirm list renders user info
        expect(screen.getByText("john@example.com")).toBeInTheDocument()

        // 2. Confirm absence of roles / groups in list view
        expect(screen.queryByTestId("realm-roles-heading")).not.toBeInTheDocument()
        expect(screen.queryByTestId("groups-heading")).not.toBeInTheDocument()

        // 3. Confirm absence of sorting controls in table headers
        const headers = screen.getAllByRole("columnheader")
        for (const header of headers) {
            expect(header.getAttribute("aria-sort")).toBeNull()
            expect(header.querySelector(".mantine-Table-sortIcon")).toBeNull()
        }
    })

    it("triggers API call and navigation/modal using Keycloak UUID, rendering returned roles and groups", async () => {
        vi.mocked(client.keycloakAdmin.getUsers).mockResolvedValue({
            data: mockUsers,
        } as unknown as Awaited<ReturnType<typeof client.keycloakAdmin.getUsers>>)
        vi.mocked(client.keycloakAdmin.getUserDetails).mockResolvedValue({
            data: mockDetail,
        } as unknown as Awaited<ReturnType<typeof client.keycloakAdmin.getUserDetails>>)

        renderWithProviders(<KeycloakUserDirectoryPage />)

        await waitFor(() => {
            expect(screen.getByTestId("detail-btn-uuid-1111-2222")).toBeInTheDocument()
        })

        // Click detail button
        fireEvent.click(screen.getByTestId("detail-btn-uuid-1111-2222"))

        // Verify API was called using exact Keycloak UUID
        expect(client.keycloakAdmin.getUserDetails).toHaveBeenCalledWith("uuid-1111-2222")

        // Verify detail modal renders roles and groups returned by detail endpoint
        await waitFor(() => {
            expect(screen.getByText("admin-realm-role")).toBeInTheDocument()
            expect(screen.getByText("user-role")).toBeInTheDocument()
            expect(screen.getByText("Engineering-Group")).toBeInTheDocument()
        })
    })

    it("renders Keycloak-unavailable error state when upstream fails", async () => {
        vi.mocked(client.keycloakAdmin.getUsers).mockRejectedValue(new Error("502 Bad Gateway"))

        renderWithProviders(<KeycloakUserDirectoryPage />)

        await waitFor(() => {
            expect(screen.getByTestId("keycloak-error-alert")).toBeInTheDocument()
        })

        expect(screen.getByText(/Keycloak Service Unavailable/i)).toBeInTheDocument()
    })
})
