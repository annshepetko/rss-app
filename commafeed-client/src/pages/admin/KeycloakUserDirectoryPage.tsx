import { Trans } from "@lingui/react/macro"
import { Badge, Box, Button, Card, Container, Group, Modal, Table, Text, TextInput, Title } from "@mantine/core"
import { useState } from "react"
import { useAsync, useAsyncCallback } from "react-async-hook"
import { TbCheck, TbChevronLeft, TbChevronRight, TbInfoCircle, TbSearch, TbX } from "react-icons/tb"
import { client, errorToStrings } from "@/app/client"
import type { KeycloakUserDetailResponse, KeycloakUserResponse } from "@/app/types"
import { Alert } from "@/components/Alert"
import { Loader } from "@/components/Loader"

function BooleanBadge({ value }: { value: boolean }) {
    return value ? (
        <Badge color="green" leftSection={<TbCheck size={12} />}>
            <Trans>Enabled</Trans>
        </Badge>
    ) : (
        <Badge color="red" leftSection={<TbX size={12} />}>
            <Trans>Disabled</Trans>
        </Badge>
    )
}

export function KeycloakUserDirectoryPage() {
    const [search, setSearch] = useState("")
    const [page, setPage] = useState(0)
    const pageSize = 10

    const [selectedId, setSelectedId] = useState<string | null>(null)

    const query = useAsync(async () => {
        return (await client.keycloakAdmin.getUsers(search, page * pageSize, pageSize)).data
    }, [search, page])

    const detailQuery = useAsyncCallback(async (id: string) => {
        return (await client.keycloakAdmin.getUserDetails(id)).data
    })

    const handleOpenDetail = (id: string) => {
        setSelectedId(id)
        detailQuery.execute(id)
    }

    const handleCloseDetail = () => {
        setSelectedId(null)
    }

    const users = query.result

    return (
        <Container size="lg">
            <Title order={3} mb="md">
                <Trans>Keycloak User Directory</Trans>
            </Title>

            <Group justify="space-between" mb="md">
                <TextInput
                    placeholder="Search users..."
                    leftSection={<TbSearch size={16} />}
                    value={search}
                    onChange={e => {
                        setSearch(e.target.value)
                        setPage(0)
                    }}
                    style={{ width: 300 }}
                />

                <Group>
                    <Button
                        variant="default"
                        leftSection={<TbChevronLeft size={16} />}
                        disabled={page === 0}
                        onClick={() => setPage(p => Math.max(0, p - 1))}
                    >
                        <Trans>Previous</Trans>
                    </Button>
                    <Text size="sm">
                        <Trans>Page {page + 1}</Trans>
                    </Text>
                    <Button
                        variant="default"
                        rightSection={<TbChevronRight size={16} />}
                        disabled={!users || users.length < pageSize}
                        onClick={() => setPage(p => p + 1)}
                    >
                        <Trans>Next</Trans>
                    </Button>
                </Group>
            </Group>

            {query.error && (
                <Box mb="md" data-testid="keycloak-error-alert">
                    <Alert
                        messages={[
                            "Keycloak Service Unavailable: Unable to connect to Keycloak Admin REST API. Please check server configuration.",
                            ...errorToStrings(query.error),
                        ]}
                    />
                </Box>
            )}

            {query.loading && !users ? (
                <Loader />
            ) : (
                <Table striped highlightOnHover data-testid="keycloak-user-table">
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>
                                <Trans>Username</Trans>
                            </Table.Th>
                            <Table.Th>
                                <Trans>Email</Trans>
                            </Table.Th>
                            <Table.Th>
                                <Trans>Name</Trans>
                            </Table.Th>
                            <Table.Th>
                                <Trans>Enabled</Trans>
                            </Table.Th>
                            <Table.Th>
                                <Trans>Actions</Trans>
                            </Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                        {users && users.length > 0 ? (
                            users.map((u: KeycloakUserResponse) => (
                                <Table.Tr key={u.keycloakId} data-testid={`user-row-${u.keycloakId}`}>
                                    <Table.Td>{u.username}</Table.Td>
                                    <Table.Td>{u.email || "-"}</Table.Td>
                                    <Table.Td>{[u.firstName, u.lastName].filter(Boolean).join(" ") || "-"}</Table.Td>
                                    <Table.Td>
                                        <BooleanBadge value={u.enabled} />
                                    </Table.Td>
                                    <Table.Td>
                                        <Button
                                            size="xs"
                                            variant="light"
                                            leftSection={<TbInfoCircle size={14} />}
                                            onClick={() => handleOpenDetail(u.keycloakId)}
                                            data-testid={`detail-btn-${u.keycloakId}`}
                                        >
                                            <Trans>Details</Trans>
                                        </Button>
                                    </Table.Td>
                                </Table.Tr>
                            ))
                        ) : (
                            <Table.Tr>
                                <Table.Td colSpan={5} style={{ textAlign: "center" }}>
                                    <Text c="dimmed">
                                        <Trans>No users found</Trans>
                                    </Text>
                                </Table.Td>
                            </Table.Tr>
                        )}
                    </Table.Tbody>
                </Table>
            )}

            <Modal opened={selectedId !== null} onClose={handleCloseDetail} title={<Trans>Keycloak User Detail</Trans>} size="lg">
                {detailQuery.loading && <Loader />}
                {detailQuery.error && <Alert messages={["Failed to load user details", ...errorToStrings(detailQuery.error)]} />}
                {detailQuery.result && <UserDetailContent user={detailQuery.result} />}
            </Modal>
        </Container>
    )
}

function UserDetailContent({ user }: { user: KeycloakUserDetailResponse }) {
    return (
        <Stack spacing="md" data-testid="user-detail-modal-content">
            <Group>
                <Text fw={500}>
                    <Trans>UUID:</Trans>
                </Text>
                <Text size="sm">{user.keycloakId}</Text>
            </Group>
            <Group>
                <Text fw={500}>
                    <Trans>Username:</Trans>
                </Text>
                <Text size="sm">{user.username}</Text>
            </Group>
            <Group>
                <Text fw={500}>
                    <Trans>Email:</Trans>
                </Text>
                <Text size="sm">{user.email || "-"}</Text>
            </Group>
            <Group>
                <Text fw={500}>
                    <Trans>Enabled:</Trans>
                </Text>
                <BooleanBadge value={user.enabled} />
            </Group>

            <Card withBorder mt="sm">
                <Text fw={600} mb="xs" data-testid="realm-roles-heading">
                    <Trans>Realm Roles</Trans>
                </Text>
                <Group gap="xs" data-testid="realm-roles-container">
                    {user.roles && user.roles.length > 0 ? (
                        user.roles.map(r => (
                            <Badge key={r} color="blue" variant="light">
                                {r}
                            </Badge>
                        ))
                    ) : (
                        <Text size="sm" c="dimmed">
                            <Trans>No realm roles</Trans>
                        </Text>
                    )}
                </Group>
            </Card>

            <Card withBorder mt="sm">
                <Text fw={600} mb="xs" data-testid="groups-heading">
                    <Trans>Groups</Trans>
                </Text>
                <Group gap="xs" data-testid="groups-container">
                    {user.groups && user.groups.length > 0 ? (
                        user.groups.map(g => (
                            <Badge key={g} color="teal" variant="light">
                                {g}
                            </Badge>
                        ))
                    ) : (
                        <Text size="sm" c="dimmed">
                            <Trans>No groups</Trans>
                        </Text>
                    )}
                </Group>
            </Card>
        </Stack>
    )
}

function Stack({ children, spacing, ...props }: { children: React.ReactNode; spacing?: string } & Record<string, unknown>) {
    return (
        <Box style={{ display: "flex", flexDirection: "column", gap: spacing === "md" ? 16 : 8 }} {...props}>
            {children}
        </Box>
    )
}
