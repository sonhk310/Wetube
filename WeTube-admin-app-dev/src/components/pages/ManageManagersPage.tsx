import React, { useState, useCallback, useRef, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  useAddManagerMutation,
  useDeleteManagerMutation,
  useGetManagersQuery,
} from "../../services/api/managerApi";
import { toast } from "react-toastify";
import type { RHFRegisterFormValues } from "../../types/managerTypes/registerManager";
import AddManagerForm from "../features/manager/AddManagerForm";
import {
  Plus,
  FileDown,
  FileUp,
  Copy,
  FileSpreadsheet,
  FileText,
  Trash2,
  Edit,
} from "lucide-react";

const Modal: React.FC<{
  isOpen: boolean;
  onClose: () => void;
  children: React.ReactNode;
}> = React.memo(({ isOpen, onClose, children }) => {
  if (!isOpen) return null;
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl p-6 w-full max-w-2xl relative shadow-lg">
        <button
          onClick={onClose}
          className="absolute top-3 right-3 text-gray-600 hover:text-black font-bold text-xl"
        >
          ×
        </button>
        {children}
      </div>
    </div>
  );
});

export const ManageManagersPage: React.FC = () => {
  const { managerId } = useParams<{ managerId?: string }>();
  const navigate = useNavigate();

  const [selectedManagerId, setSelectedManagerId] = useState<string | null>(
    managerId || null
  );
  const [formState, setFormState] = useState<{
    showAddForm: boolean;
    managerInfo: RHFRegisterFormValues;
  }>(() => ({
    showAddForm: false,
    managerInfo: {
      name: "",
      email: "",
      phone: "",
      password: "",
      confirmPassword: "",
      gender: "male",
      dateOfBirth: "",
      address: "",
      role: "Manager",
      status: "Pending",
    },
  }));

  // pagination
  const [currentPage, setCurrentPage] = useState(1);
  const managersPerPage = 10;

  const {
    data: managers = [],
    isLoading: isManagersLoading,
    error: managersError,
  } = useGetManagersQuery();

  const [addManager] = useAddManagerMutation();
  const [deleteManager] = useDeleteManagerMutation();
  const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);

  // đảm bảo ít nhất 1 trang để pagination luôn hiển thị
  const totalPages = Math.max(1, Math.ceil(managers.length / managersPerPage));
  const currentManagers = managers.slice(
    (currentPage - 1) * managersPerPage,
    currentPage * managersPerPage
  );

  const tableScrollRef = useRef<HTMLDivElement | null>(null);

  // Khi chuyển trang, scroll lên đầu phần bảng (nếu có scroll)
  useEffect(() => {
    if (tableScrollRef.current) {
      tableScrollRef.current.scrollTop = 0;
    }
  }, [currentPage]);

  const handleAddManager = useCallback(
    async (data: RHFRegisterFormValues) => {
      try {
        await addManager(data).unwrap();
        toast.success("Manager added successfully!");
        setFormState((prev) => ({ ...prev, showAddForm: false }));
      } catch {
        toast.error("Failed to add manager!");
      }
    },
    [addManager]
  );

  const handleDeleteManager = useCallback(async () => {
    if (!deleteTargetId) return;
    try {
      await deleteManager(deleteTargetId).unwrap();
      toast.success("Manager deleted successfully!");
      if (selectedManagerId === deleteTargetId) setSelectedManagerId(null);
    } catch {
      toast.error("Failed to delete manager!");
    } finally {
      setDeleteTargetId(null);
    }
  }, [deleteManager, deleteTargetId, selectedManagerId]);

  return (
    <div className="h-173 w-full p-6 bg-gradient-to-br from-green-300 via-blue-200 to-yellow-100 dark:bg-[#034c5f]">
      <div className="h-full flex flex-col">
      <div className="flex gap-2 mb-4 flex-wrap">
        <button
          onClick={() =>
            setFormState((prev) => ({ ...prev, showAddForm: true }))
          }
          className="px-3 py-2 bg-green-600 text-white rounded-md flex items-center gap-2 hover:bg-green-700"
        >
          <Plus size={18} /> Add New Manager
        </button>
        <button className="px-3 py-2 bg-yellow-500 text-white rounded-md flex items-center gap-2 hover:bg-yellow-600">
          <FileUp size={18} /> Upload from File
        </button>
        <button className="px-3 py-2 bg-blue-500 text-white rounded-md flex items-center gap-2 hover:bg-blue-600">
          <FileDown size={18} /> Download Data
        </button>
        <button className="px-3 py-2 bg-indigo-500 text-white rounded-md flex items-center gap-2 hover:bg-indigo-600">
          <Copy size={18} /> Copy
        </button>
        <button className="px-3 py-2 bg-green-500 text-white rounded-md flex items-center gap-2 hover:bg-green-600">
          <FileSpreadsheet size={18} /> Export Excel
        </button>
        <button className="px-3 py-2 bg-red-500 text-white rounded-md flex items-center gap-2 hover:bg-red-600">
          <FileText size={18} /> Export PDF
        </button>
        <button className="px-3 py-2 bg-gray-500 text-white rounded-md flex items-center gap-2 hover:bg-gray-600">
          <Trash2 size={18} /> Delete All
        </button>
      </div>

      <div
        ref={tableScrollRef}
        className="bg-white rounded-lg shadow mb-6 max-h-[60vh] overflow-auto"
      >
        <table className="w-full border-collapse">
          <thead className="bg-gray-100 text-gray-700">
            <tr>
              <th className="p-2 border">Manager ID</th>
              <th className="p-2 border">Name</th>
              <th className="p-2 border">Avatar</th>
              <th className="p-2 border">Address</th>
              <th className="p-2 border">Date of Birth</th>
              <th className="p-2 border">Gender</th>
              <th className="p-2 border">Phone</th>
              <th className="p-2 border">Role</th>
              <th className="p-2 border">Actions</th>
            </tr>
          </thead>

          <tbody>
            {currentManagers.map((m: any) => (
              <tr key={m.id} className="text-center">
                <td className="border p-2">{m.id}</td>
                <td className="border p-2">{m.name}</td>
                <td className="border p-2">
                  <img
                    src={m.avatarUrl || "/default-avatar.png"}
                    alt={m.name}
                    className="w-10 h-10 rounded-full mx-auto"
                  />
                </td>
                <td className="border p-2">{m.address}</td>
                <td className="border p-2">{m.dateOfBirth}</td>
                <td className="border p-2">{m.gender}</td>
                <td className="border p-2">{m.phone}</td>
                <td className="border p-2">{m.role}</td>
                <td className="border p-2 flex justify-center gap-2">
                  <button className="p-2 bg-yellow-400 rounded hover:bg-yellow-500">
                    <Edit size={16} />
                  </button>
                  <button
                    onClick={() => setDeleteTargetId(m.id)}
                    className="p-2 bg-red-500 text-white rounded hover:bg-red-600"
                  >
                    <Trash2 size={16} />
                  </button>
                </td>
              </tr>
            ))}
            {currentManagers.length === 0 && (
              <tr>
                <td colSpan={9} className="p-4 text-center text-gray-500">
                  Không có dữ liệu
                </td>
              </tr>
            )}
          </tbody>

          {/* Pagination nằm trong tfoot để hiển thị "bên trong" table */}
          <tfoot>
            <tr className="sticky bottom-0">
              <td colSpan={9} className="bg-white border-t p-3">
                <div className="flex justify-center gap-2">
                  {Array.from({ length: totalPages }, (_, i) => i + 1).map(
                    (page) => (
                      <button
                        key={page}
                        onClick={() => setCurrentPage(page)}
                        className={`px-3 py-1 rounded-md ${
                          page === currentPage
                            ? "bg-blue-500 text-white"
                            : "bg-gray-200 text-gray-700 hover:bg-gray-300"
                        }`}
                      >
                        {page}
                      </button>
                    )
                  )}
                </div>
              </td>
            </tr>
          </tfoot>
        </table>
      </div>

      {/* Add Manager Modal */}
      <Modal
        isOpen={formState.showAddForm}
        onClose={() =>
          setFormState((prev) => ({ ...prev, showAddForm: false }))
        }
      >
        <AddManagerForm onSubmit={handleAddManager} />
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal isOpen={!!deleteTargetId} onClose={() => setDeleteTargetId(null)}>
        <div className="space-y-4">
          <h2 className="text-xl font-bold text-gray-800">
            Delete Confirmation
          </h2>
          <p>Are you sure you want to delete this manager?</p>
          <div className="flex justify-end gap-4">
            <button
              onClick={() => setDeleteTargetId(null)}
              className="px-4 py-2 rounded-md bg-gray-300 hover:bg-gray-400"
            >
              Cancel
            </button>
            <button
              onClick={handleDeleteManager}
              className="px-4 py-2 rounded-md bg-red-600 text-white hover:bg-red-700"
            >
              Delete
            </button>
          </div>
        </div>
      </Modal>
      </div>
    </div>
  );
};
