import { EyeIcon, PencilSquareIcon, TrashIcon, ExclamationTriangleIcon } from "@heroicons/react/24/outline";
import type { managerData } from "../../../types/managerTypes/managerInfo";

export const ManagerList: React.FC<{
  managers: managerData[];
  selectedManagerId: string | null;
  onSelect: (id: string) => void;
  onView: (id: string) => void;
  onEdit: (id: string) => void;
  onDelete: (id: string) => void;
  onBanned: (id: string) => void;
}> = ({ managers, selectedManagerId, onSelect,onView ,onEdit , onDelete, onBanned }) => {
  return (
    <div className="w-full p-5 bg-white rounded-xl shadow-md overflow-x-auto">
      <h2 className="text-xl font-bold mb-4 text-gray-900">Manager List</h2>

      {managers.length === 0 ? (
        <p className="text-gray-500 text-center">No managers available.</p>
      ) : (
        <ul className="space-y-3 max-h-[500px] overflow-y-auto pr-2">
          {managers.map((manager) => (
            <li
              key={manager.id}
              onClick={() => onSelect(manager.id)}
              className={`flex justify-between items-center p-4 rounded-lg cursor-pointer transition ${
                selectedManagerId === manager.id ? "bg-blue-100" : "hover:bg-gray-100"
              }`}
            >

              <div className="flex items-center gap-4 w-full">
                <img
                  src={manager.avatarUrl || "https://via.placeholder.com/40"}
                  alt={`${manager.name}'s avatar`}
                  className="w-12 h-12 rounded-full object-cover"
                />
                <div className="flex flex-col w-full">
                  <div className="flex justify-between">
                    <p className="font-semibold text-gray-800">{manager.name}</p>
                    <span
                      className={`text-sm font-medium ${
                        manager.status === "Approved"
                          ? "text-green-600"
                          : manager.status === "Rejected"
                          ? "text-red-600"
                          : manager.status === "Banned"
                          ? "text-red-500"
                          : "text-yellow-600"
                      }`}
                    >
                      {manager.status}
                    </span>
                  </div>
                  <p className="text-xs text-gray-600">{manager.email}</p>
                </div>
              </div>

              <div className="flex gap-2 shrink-0 pl-4">

                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onView(manager.id);
                    }}
                    className="p-2 rounded-md hover:bg-blue-100 transition"
                    title="View"
                  >
                    <EyeIcon className="w-5 h-5 text-gray-500 hover:text-blue-600" />
                  </button>

                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onEdit(manager.id);
                    }}
                    className="p-2 rounded-md hover:bg-green-100 transition"
                    title="Edit"
                  >
                    <PencilSquareIcon className="w-5 h-5 text-gray-500 hover:text-green-600" />
                </button>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onDelete(manager.id);
                  }}
                  className="p-2 rounded-md hover:bg-red-100 transition"
                  title="Delete"
                >
                  <TrashIcon className="w-5 h-5 text-gray-500 hover:text-red-600" />
                </button>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onBanned(manager.id);
                  }}
                  className="p-2 rounded-md hover:bg-yellow-100 transition"
                  title="deactive"
                >
                  <ExclamationTriangleIcon className="w-5 h-5 text-gray-500 hover:text-yellow-600" />
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}; 
